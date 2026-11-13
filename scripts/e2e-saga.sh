#!/usr/bin/env bash
# Percorre a Saga de ponta a ponta contra os três microsserviços em execução:
#   1) fluxo feliz: OS aberta → diagnóstico → orçamento → aprovação → pagamento → execução → entrega
#   2) falha: estoque consumido após o diagnóstico → execução falha → estorno do pagamento
# Variáveis: OS_URL, BILLING_URL, EXECUTION_URL, ADMIN_USER, ADMIN_PASSWORD
set -euo pipefail

OS_URL=${OS_URL:-http://localhost:8080}
BILLING_URL=${BILLING_URL:-http://localhost:8081}
EXECUTION_URL=${EXECUTION_URL:-http://localhost:8082}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin123}

azul() { printf '\n\033[1;34m== %s\033[0m\n' "$*"; }
ok() { printf '\033[32m✔ %s\033[0m\n' "$*"; }
falha() { printf '\033[31m✘ %s\033[0m\n' "$*"; exit 1; }

for url in "$OS_URL" "$BILLING_URL" "$EXECUTION_URL"; do
  curl -fsS --retry 30 --retry-delay 3 --retry-all-errors "$url/actuator/health/readiness" > /dev/null
done

TOKEN=$(curl -fsS -X POST "$OS_URL/auth/login" -H 'Content-Type: application/json' \
  -d "{\"usuario\":\"$ADMIN_USER\",\"senha\":\"$ADMIN_PASSWORD\"}")
AUTH="Authorization: Bearer $TOKEN"
JSON='Content-Type: application/json'
api() { local metodo=$1 url=$2; shift 2; curl -fsS -X "$metodo" "$url" -H "$AUTH" -H "$JSON" "$@"; }

# Aguarda até a OS atingir o status esperado (os eventos trafegam de forma assíncrona).
esperar_status() {
  local os=$1 esperado=$2 atual=""
  for _ in $(seq 1 40); do
    atual=$(api GET "$OS_URL/ordens-servico/$os" | jq -r .status)
    [ "$atual" = "$esperado" ] && { ok "OS em $esperado"; return 0; }
    sleep 0.5
  done
  falha "OS deveria estar em $esperado, mas está em $atual"
}
esperar_execucao() {
  local os=$1 esperado=$2 atual=""
  for _ in $(seq 1 40); do
    atual=$(api GET "$EXECUTION_URL/execucoes/$os" 2>/dev/null | jq -r .status || true)
    [ "$atual" = "$esperado" ] && return 0
    sleep 0.5
  done
  falha "Execução deveria estar em $esperado, mas está em $atual"
}
esperar_orcamento() {
  local os=$1 id=""
  for _ in $(seq 1 40); do
    id=$(api GET "$BILLING_URL/orcamentos/os/$os" 2>/dev/null | jq -r .id || true)
    [ -n "$id" ] && [ "$id" != "null" ] && { echo "$id"; return 0; }
    sleep 0.5
  done
  falha "Orçamento da OS $os não foi gerado"
}
esperar_saga() {
  local os=$1 esperado=$2 atual=""
  for _ in $(seq 1 40); do
    atual=$(api GET "$OS_URL/ordens-servico/$os/saga" | jq -r .status)
    [ "$atual" = "$esperado" ] && { ok "Saga $esperado"; return 0; }
    sleep 0.5
  done
  falha "Saga deveria estar $esperado, mas está $atual"
}

CPF=$(python3 -c "
import random
n=[random.randint(0,9) for _ in range(9)]
for _ in range(2):
    s=sum(v*f for v,f in zip(n, range(len(n)+1,1,-1))); d=(s*10)%11; n.append(0 if d==10 else d)
print(''.join(map(str,n)))")
PLACA=$(python3 -c "import random,string; L=string.ascii_uppercase; print(''.join(random.choices(L,k=3))+str(random.randint(0,9))+random.choice(L)+f'{random.randint(0,99):02d}')")

azul "Cadastros (os-service e catálogo no execution-service)"
api POST "$OS_URL/clientes" -d "{\"nome\":\"Cliente E2E\",\"cpf\":\"$CPF\",\"telefone\":\"41999990000\",\"email\":\"e2e@oficina.com\"}" > /dev/null
api POST "$OS_URL/veiculos" -d "{\"marca\":\"Fiat\",\"modelo\":\"Argo\",\"ano\":2022,\"cor\":\"Prata\",\"placa\":\"$PLACA\",\"cpfProprietario\":\"$CPF\"}" > /dev/null
SERVICO=$(api POST "$EXECUTION_URL/servicos" -d '{"nome":"Troca de pastilhas","preco":150.00,"tempoEstimadoMinutos":60}' | jq -r .id)
PASTILHA=$(api POST "$EXECUTION_URL/insumos" -d '{"nome":"Pastilha de freio","tipo":"PECA","preco":80.00,"quantidadeEstoque":4}' | jq -r .id)
ok "Cliente $CPF, veículo $PLACA, serviço e insumo cadastrados"

abrir_e_diagnosticar() {
  local os
  os=$(api POST "$OS_URL/ordens-servico" -d "{\"cpfCliente\":\"$CPF\",\"placaVeiculo\":\"$PLACA\",\"sintomaRelatado\":\"Barulho ao frear\"}" | jq -r .id)
  esperar_execucao "$os" AGUARDANDO_DIAGNOSTICO
  api POST "$EXECUTION_URL/execucoes/$os/diagnostico/iniciar" > /dev/null
  esperar_status "$os" EM_DIAGNOSTICO >&2
  api POST "$EXECUTION_URL/execucoes/$os/diagnostico/itens" -d "{\"tipo\":\"SERVICO\",\"referenciaId\":\"$SERVICO\",\"quantidade\":1}" > /dev/null
  api POST "$EXECUTION_URL/execucoes/$os/diagnostico/itens" -d "{\"tipo\":\"INSUMO\",\"referenciaId\":\"$PASTILHA\",\"quantidade\":2}" > /dev/null
  api POST "$EXECUTION_URL/execucoes/$os/diagnostico/concluir" > /dev/null
  esperar_status "$os" AGUARDANDO_APROVACAO >&2
  echo "$os"
}

pagar() {
  local orcamento=$1 resultado=$2 pagamento
  pagamento=$(api POST "$BILLING_URL/orcamentos/$orcamento/aprovar")
  echo "   link de pagamento: $(echo "$pagamento" | jq -r .linkPagamento)" >&2
  curl -fsS -X POST "$BILLING_URL/pagamentos/$(echo "$pagamento" | jq -r .id)/simular?resultado=$resultado" > /dev/null
}

azul "1) Fluxo feliz"
OS1=$(abrir_e_diagnosticar)
ORC1=$(esperar_orcamento "$OS1")
ok "Orçamento $ORC1 gerado: R$ $(api GET "$BILLING_URL/orcamentos/$ORC1" | jq -r .valorTotal)"
pagar "$ORC1" APROVADO
esperar_status "$OS1" AGUARDANDO_EXECUCAO
esperar_execucao "$OS1" AGUARDANDO_EXECUCAO
ok "Estoque reservado: pastilhas restantes = $(api GET "$EXECUTION_URL/insumos/$PASTILHA" | jq -r .quantidadeEstoque)"
api POST "$EXECUTION_URL/execucoes/$OS1/reparo/iniciar" > /dev/null
esperar_status "$OS1" EM_EXECUCAO
api POST "$EXECUTION_URL/execucoes/$OS1/reparo/finalizar" > /dev/null
esperar_status "$OS1" FINALIZADA
esperar_saga "$OS1" CONCLUIDA
api PATCH "$OS_URL/ordens-servico/$OS1/entrega" > /dev/null
esperar_status "$OS1" ENTREGUE

azul "2) Falha na execução → compensação com estorno"
OS2=$(abrir_e_diagnosticar)
ORC2=$(esperar_orcamento "$OS2")
# Outra OS (ex.: de outra filial) paga antes e consome as últimas pastilhas entre o
# diagnóstico e o pagamento desta OS — a reserva de estoque vai falhar.
OS_EXTRA=$(abrir_e_diagnosticar)
ORC_EXTRA=$(esperar_orcamento "$OS_EXTRA")
pagar "$ORC_EXTRA" APROVADO
esperar_execucao "$OS_EXTRA" AGUARDANDO_EXECUCAO
ok "Estoque esgotado: $(api GET "$EXECUTION_URL/insumos/$PASTILHA" | jq -r .quantidadeEstoque) pastilha(s)"
pagar "$ORC2" APROVADO
esperar_status "$OS2" CANCELADA
esperar_saga "$OS2" COMPENSADA
[ "$(api GET "$BILLING_URL/orcamentos/$ORC2" | jq -r .status)" = "ESTORNADO" ] && ok "Pagamento estornado no billing-service"
echo "   motivo: $(api GET "$OS_URL/ordens-servico/$OS2" | jq -r .motivoCancelamento)"

azul "3) Cliente rejeita o orçamento → compensação sem estorno"
api POST "$EXECUTION_URL/insumos/$PASTILHA/reposicao" -d '{"quantidade":10}' > /dev/null
ok "Estoque reposto: $(api GET "$EXECUTION_URL/insumos/$PASTILHA" | jq -r .quantidadeEstoque) pastilha(s)"
OS3=$(abrir_e_diagnosticar)
ORC3=$(esperar_orcamento "$OS3")
api POST "$BILLING_URL/orcamentos/$ORC3/rejeitar" -d '{"motivo":"Valor acima do esperado"}' > /dev/null
esperar_status "$OS3" CANCELADA
esperar_saga "$OS3" COMPENSADA

azul "Log da Saga da OS com estorno ($OS2)"
api GET "$OS_URL/ordens-servico/$OS2/saga" | jq -r '.eventos[] | "\(.dataHora)  \(.tipo)  \(.descricao // "")"'
printf '\n\033[1;32mE2E OK — fluxo feliz, falha com estorno e rejeição validados.\033[0m\n'
