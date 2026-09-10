# Fintech de pagamentos com gRPC

Trabalho de Sistemas Distribuídos. Dois serviços Java com gRPC, sem Spring:

- `pagamento-service` recebe a solicitação de processamento de uma transação.
- `conta-service` valida e processa a transação contra os saldos das contas.
- `contratos-grpc` guarda os arquivos `.proto` e gera as classes Java.

O cliente é um menu de terminal em `ClientePagamentosGrpc`.

Compilar e executar:

```bash
./mvnw clean install
./mvnw -pl conta-service exec:java -Dexec.mainClass=aula.sd.fintech.conta.ServidorContasGrpc
./mvnw -pl pagamento-service exec:java -Dexec.mainClass=aula.sd.fintech.pagamento.ServidorPagamentosGrpc
./mvnw -pl pagamento-service exec:java -Dexec.mainClass=aula.sd.fintech.pagamento.ClientePagamentosGrpc
```

O guia completo, incluindo a execução com uma VM no Google Cloud, está em
[docs/pagamentos.md](docs/pagamentos.md).
