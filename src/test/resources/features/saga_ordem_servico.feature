# language: pt
Funcionalidade: Saga orquestrada da Ordem de Serviço
  Como oficina mecânica com múltiplas filiais
  Quero que o fluxo da OS seja coordenado entre os microsserviços
  Para garantir consistência e rollback seguro quando alguma etapa falhar

  Contexto:
    Dado um cliente cadastrado com um veículo
    E que o atendente abriu uma ordem de serviço

  Cenário: Fluxo completo da OS passando por todos os microsserviços
    Então o comando "execution.iniciar-diagnostico" deve ser enviado
    Quando o execution-service publicar "execution.diagnostico-iniciado"
    Então a OS deve estar com status "EM_DIAGNOSTICO"
    Quando o execution-service concluir o diagnóstico com itens no valor de "310.00"
    Então a OS deve estar com status "AGUARDANDO_APROVACAO"
    E o comando "billing.gerar-orcamento" deve ser enviado
    Quando o billing-service publicar "billing.orcamento-gerado"
    E o billing-service publicar "billing.orcamento-aprovado"
    Então a OS deve estar com status "AGUARDANDO_PAGAMENTO"
    Quando o billing-service publicar "billing.pagamento-confirmado"
    Então a OS deve estar com status "AGUARDANDO_EXECUCAO"
    E o comando "execution.iniciar-execucao" deve ser enviado
    Quando o execution-service publicar "execution.execucao-iniciada"
    E o execution-service publicar "execution.execucao-finalizada"
    Então a OS deve estar com status "FINALIZADA"
    E a Saga deve estar com status "CONCLUIDA"
    E o histórico da OS deve conter os status:
      | RECEBIDA             |
      | EM_DIAGNOSTICO       |
      | AGUARDANDO_APROVACAO |
      | AGUARDANDO_PAGAMENTO |
      | AGUARDANDO_EXECUCAO  |
      | EM_EXECUCAO          |
      | FINALIZADA           |

  Cenário: Cliente rejeita o orçamento
    Dado que a OS está aguardando aprovação do orçamento
    Quando o billing-service publicar "billing.orcamento-rejeitado"
    Então a OS deve estar com status "CANCELADA"
    E a Saga deve estar com status "COMPENSANDO"
    E o comando "billing.cancelar-orcamento" deve ser enviado
    E o comando "execution.cancelar-execucao" deve ser enviado
    E o comando "billing.estornar-pagamento" não deve ser enviado
    Quando o billing-service publicar "billing.orcamento-cancelado"
    E o execution-service publicar "execution.execucao-cancelada"
    Então a Saga deve estar com status "COMPENSADA"

  Cenário: Falha na execução por falta de estoque estorna o pagamento
    Dado que a OS está com o pagamento confirmado
    Quando o execution-service publicar "execution.execucao-falhou" com motivo "Estoque insuficiente"
    Então a OS deve estar com status "CANCELADA"
    E o motivo do cancelamento deve conter "Estoque insuficiente"
    E o comando "billing.estornar-pagamento" deve ser enviado
    E o comando "execution.cancelar-execucao" deve ser enviado
    Quando o billing-service publicar "billing.pagamento-estornado"
    E o execution-service publicar "execution.execucao-cancelada"
    Então a Saga deve estar com status "COMPENSADA"

  Cenário: Pagamento recusado pelo Mercado Pago
    Dado que a OS está aguardando aprovação do orçamento
    Quando o billing-service publicar "billing.orcamento-aprovado"
    E o billing-service publicar "billing.pagamento-recusado" com motivo "cc_rejected_other_reason"
    Então a OS deve estar com status "CANCELADA"
    E o comando "billing.cancelar-orcamento" deve ser enviado
    E o comando "billing.estornar-pagamento" não deve ser enviado

  Cenário: Prazo de pagamento expira sem ação do cliente
    Dado que a OS está aguardando aprovação do orçamento
    Quando o billing-service publicar "billing.orcamento-aprovado"
    E se passarem 25 horas sem pagamento
    Então a OS deve estar com status "CANCELADA"
    E o comando "billing.cancelar-orcamento" deve ser enviado
