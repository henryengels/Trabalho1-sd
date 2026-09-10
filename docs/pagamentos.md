# Fintech de pagamentos via gRPC

## 1. Conceitos importantes

- `contratos-grpc` contém os arquivos `.proto` compartilhados e gera as classes
  Java usadas pelos outros módulos.
- `pagamento-service` recebe a transação do cliente, gera um identificador e
  pede ao serviço de contas que valide e efetive a transferência. Também repassa
  consultas de saldo.
- `conta-service` guarda os saldos em um mapa em memória e é o único ponto que
  altera as contas. Ele recusa a transação quando o valor é inválido, quando
  origem e destino são iguais, quando alguma conta não existe ou quando o saldo
  é insuficiente.
- Valores monetários trafegam em centavos (`int64`) para evitar erros de
  arredondamento de ponto flutuante. O cliente converte `150.50` em `15050`.
- `pagamento.proto` importa `conta.proto` para reaproveitar as mensagens de
  consulta de saldo. Os dois arquivos geram classes no mesmo pacote Java.
- O cliente chama o servidor de pagamentos na porta 9090. O servidor de
  pagamentos atua como cliente do serviço de contas na porta 9091.
- As classes geradas ficam somente em
  `contratos-grpc/target/generated-sources/protobuf/`. Não copie nem edite essas
  classes em `src/main/java`.
- Se o serviço de contas estiver desligado, a chamada falha com o erro gRPC
  padrão. Não há fallback, retry ou tratamento especial.

## 2. Arquitetura e configurações necessárias

```text
ClientePagamentosGrpc (menu no terminal)
        |
        | gRPC :9090
        v
ServidorPagamentosGrpc
        |
        | ClienteContasGrpc
        | gRPC :9091
        v
ServidorContasGrpc
        |
        v
Map: ana = R$ 1.000,00, bruno = R$ 500,00, carla = R$ 0,00
```

Módulos Maven:

| Módulo              | Responsabilidade                                                 |
|---------------------|------------------------------------------------------------------|
| `contratos-grpc`    | Contratos `pagamento.proto` e `conta.proto` e código gerado      |
| `pagamento-service` | Servidor de pagamentos, cliente de terminal e cliente das contas |
| `conta-service`     | Servidor e implementação dos saldos e da transferência           |

Métodos remotos:

| Serviço            | Método               | Quem chama              |
|--------------------|----------------------|-------------------------|
| `PagamentoService` | `ProcessarTransacao` | `ClientePagamentosGrpc` |
| `PagamentoService` | `ConsultarSaldo`     | `ClientePagamentosGrpc` |
| `ContaService`     | `Transferir`         | `PagamentoServiceImpl`  |
| `ContaService`     | `ConsultarSaldo`     | `PagamentoServiceImpl`  |

Valores fixados diretamente no código:

| Classe                   | Constante      | Valor local |
|--------------------------|----------------|-------------|
| `ServidorPagamentosGrpc` | `PORTA`        | `9090`      |
| `ClientePagamentosGrpc`  | `HOST`         | `localhost` |
| `ClientePagamentosGrpc`  | `PORTA`        | `9090`      |
| `PagamentoServiceImpl`   | `CONTAS_HOST`  | `localhost` |
| `PagamentoServiceImpl`   | `CONTAS_PORTA` | `9091`      |
| `ServidorContasGrpc`     | `PORTA`        | `9091`      |

Host e portas não são recebidos por argumentos nem por variáveis de ambiente.
Para alterar um desses valores, edite a constante na classe indicada e recompile
o projeto. Contas e valores são digitados no menu do cliente.

## 3. Como compilar

Execute localmente, na raiz do projeto:

```bash
./mvnw clean install
```

O Maven compila os módulos nesta ordem:

```text
fintech
contratos-grpc
pagamento-service
conta-service
```

O resultado esperado termina com:

```text
BUILD SUCCESS
```

Se aparecer `Unable to clean up temporary proto file directory` no Windows,
execute o comando novamente. É um bloqueio temporário de arquivo, não um erro
de código.

## 4. Como executar

Abra três terminais na raiz do projeto e respeite a ordem abaixo.

No primeiro terminal, inicie localmente o serviço de contas:

```bash
./mvnw -pl conta-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.conta.ServidorContasGrpc
```

No segundo terminal, inicie localmente o serviço de pagamentos:

```bash
./mvnw -pl pagamento-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.pagamento.ServidorPagamentosGrpc
```

No terceiro terminal, execute localmente o cliente:

```bash
./mvnw -pl pagamento-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.pagamento.ClientePagamentosGrpc
```

O cliente mostra o menu:

```text
=== Fintech de Pagamentos ===
1 - Consultar saldo
2 - Realizar transferência
0 - Sair
Opção:
```

Roteiro sugerido para a demonstração:

1. Opção `1`, conta `ana`: saldo de R$ 1.000,00.
2. Opção `2`, origem `ana`, destino `bruno`, valor `150.50`: transação
   `APROVADA`.
3. Opção `1`, conta `bruno`: saldo de R$ 650,50.
4. Opção `2`, origem `bruno`, destino `ana`, valor `10000`: `RECUSADA` por
   saldo insuficiente.
5. Opção `2`, origem `ana`, destino `zeca`, valor `5`: `RECUSADA` porque a conta
   de destino não existe.
6. Opção `0` para sair.

Os saldos ficam em memória. Reiniciar o serviço de contas restaura os valores
iniciais.

## 5. Resultados esperados

O servidor de contas inicia com:

```text
Servidor de contas gRPC ouvindo na porta 9091
```

O servidor de pagamentos inicia com:

```text
Servidor de pagamentos gRPC ouvindo na porta 9090
```

Para a transferência de R$ 150,50 de `ana` para `bruno`, o cliente mostra uma
resposta semelhante a:

```text
Transação ID: bc6ac013-6953-4104-8a59-043bb957ecc3
Status: APROVADA
Mensagem: Transferência efetivada.
Saldo da origem: R$ 849,50
```

O servidor de pagamentos imprime:

```text
Transação bc6ac013-6953-4104-8a59-043bb957ecc3 recebida: origem=ana, destino=bruno, valor=R$ 150,50
Transação bc6ac013-6953-4104-8a59-043bb957ecc3 APROVADA: Transferência efetivada.
```

O servidor de contas imprime:

```text
Transferência recebida: origem=ana, destino=bruno, valor=R$ 150,50
Resultado: aprovada=true, mensagem=Transferência efetivada.
```

Para R$ 10.000,00 de `bruno` para `ana`, o resultado esperado é:

```text
Transação ID: b4f37ae3-7607-456f-b79b-18a359f38711
Status: RECUSADA
Mensagem: Saldo insuficiente.
Saldo da origem: R$ 650,50
```

O UUID muda a cada chamada.

## 6. Execução no Google Cloud

O exemplo usa uma única VM. Os dois servidores rodam na mesma máquina, por isso
`CONTAS_HOST = "localhost"` não precisa ser alterado. Somente a porta 9090
precisa estar liberada no firewall para o IP do cliente.

Caso use mais de uma VM e necessite trocar `CONTAS_HOST = IP_INTERNO_VM`. Edite no código dentro da vm que irá fazer a requisição para o módulo de contas

```bash
sed -i 's/CONTAS_HOST = "localhost"/CONTAS_HOST = "IP_INTERNO_A_SER_SUBSTITUIDO"/' \
pagamento-service\src\main\java\aula\sd\fintech\pagamento\PagamentoServiceImpl.java
```

Valide se a alteração foi aplicada com:

```bash
git diff
```

Se digitar o IP errado na hora de executar o comando, apenas reverta com o seguinte comando:

```bash
git checkout .
```

Acesse a VM por SSH e instale as dependências:

```bash
sudo apt update
sudo apt install -y git openjdk-21-jdk
java -version
```

O comando `java -version` deve informar a versão 21.

Clone o projeto e compile:

```bash
git clone <link do github>
cd <pasta do projeto>
./mvnw clean install
```

Abra dois terminais SSH na VM. No primeiro, inicie o serviço de contas:

```bash
./mvnw -pl conta-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.conta.ServidorContasGrpc
```

No segundo, inicie o serviço de pagamentos:

```bash
./mvnw -pl pagamento-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.pagamento.ServidorPagamentosGrpc
```

Se preferir um único terminal na VM, deixe o serviço de contas em segundo plano:

```bash
nohup ./mvnw -pl conta-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.conta.ServidorContasGrpc > contas.log 2>&1 &
```

Edite localmente
`pagamento-service/src/main/java/aula/sd/fintech/pagamento/ClientePagamentosGrpc.java`
e troque o valor de `HOST` pelo IP público mostrado no console do GCP para a VM,
por exemplo:

```java
private static final String HOST = "34.123.45.67";
```

O número acima é apenas um exemplo. Depois da alteração, execute localmente:

```bash
./mvnw -pl pagamento-service compile
./mvnw -pl pagamento-service exec:java \
  -Dexec.mainClass=aula.sd.fintech.pagamento.ClientePagamentosGrpc
```

O cliente usa plaintext porque o exemplo demonstra somente o fluxo básico de
gRPC e Protobuf.

## 7. Erros comuns e identificação

| Sintoma                                              | Como identificar                                                        | Ação                                                              |
|------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------|
| `Falha na chamada gRPC: Status{code=UNAVAILABLE...}` | Servidor de pagamentos parado, `HOST` incorreto ou porta 9090 bloqueada | Inicie o servidor, confira a constante e a regra de firewall      |
| `UNAVAILABLE` no servidor de pagamentos              | Serviço de contas parado ou porta 9091 ocupada por outro processo       | Inicie o serviço de contas antes do de pagamentos                 |
| `Connection refused`                                 | Nenhum processo está ouvindo na porta informada                         | Respeite a ordem contas → pagamentos → cliente                    |
| Dependência `contratos-grpc` não encontrada          | Os módulos não foram instalados no repositório Maven local              | Execute `./mvnw clean install` na raiz                            |
| Classe gRPC gerada não encontrada                    | A geração Protobuf não foi executada no módulo de contratos             | Execute `./mvnw clean install` e confira `contratos-grpc/target/` |
| Imports `aula.sd.fintech.grpc` vermelhos no IntelliJ | O modelo Maven ainda não foi recarregado                                | Na janela Maven, clique em `Reload All Maven Projects`            |
| Erro de versão do Java                               | `java -version` não mostra 21                                           | Instale ou selecione o JDK 21                                     |
| `Valor inválido` no cliente                          | Texto digitado não é um número ou tem mais de duas casas decimais       | Digite no formato `150.50` ou `150,50`                            |
| Acentos trocados no terminal do Windows              | O console não está em UTF-8                                             | Execute `chcp 65001` antes de rodar o cliente                     |

## 8. Encerramento e limpeza dos recursos

Interrompa os servidores com `Ctrl+C`. Se o serviço de contas estiver em segundo
plano, encerre com:

```bash
pkill -f ServidorContasGrpc
```

Depois, encerre a conexão SSH e pare a VM via o console do GCP:

```bash
exit
```
