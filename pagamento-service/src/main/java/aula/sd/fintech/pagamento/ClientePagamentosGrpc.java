package aula.sd.fintech.pagamento;

import aula.sd.fintech.grpc.ConsultarSaldoRequest;
import aula.sd.fintech.grpc.ConsultarSaldoResponse;
import aula.sd.fintech.grpc.PagamentoServiceGrpc;
import aula.sd.fintech.grpc.ProcessarTransacaoRequest;
import aula.sd.fintech.grpc.ProcessarTransacaoResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.math.BigDecimal;
import java.util.Scanner;

// Cliente de terminal: abre um canal, mostra um menu e chama os métodos remotos.
public class ClientePagamentosGrpc {

    // Troque pelo IP público da VM ao executar contra o Google Cloud.
    private static final String HOST = "localhost";
    private static final int PORTA = 9090;

    public static void main(String[] args) {
        // O canal mantém a conexão usada pelo cliente para se comunicar com o servidor.
        ManagedChannel canal = ManagedChannelBuilder.forAddress(HOST, PORTA).usePlaintext().build();

        // O stub bloqueante aguarda a resposta antes de continuar a execução.
        PagamentoServiceGrpc.PagamentoServiceBlockingStub stub = PagamentoServiceGrpc.newBlockingStub(canal);

        Scanner entrada = new Scanner(System.in);
        System.out.println("Conectando em " + HOST + ":" + PORTA);

        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("=== Fintech de Pagamentos ===");
            System.out.println("1 - Consultar saldo");
            System.out.println("2 - Realizar transferência");
            System.out.println("0 - Sair");
            System.out.print("Opção: ");

            if (!entrada.hasNextLine()) {
                break;
            }
            String opcao = entrada.nextLine().trim();

            try {
                switch (opcao) {
                    case "1" -> consultarSaldo(stub, entrada);
                    case "2" -> realizarTransferencia(stub, entrada);
                    case "0" -> continuar = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (StatusRuntimeException e) {
                // Erro padrão do gRPC: servidor parado, host errado ou porta bloqueada.
                System.out.println("Falha na chamada gRPC: " + e.getStatus());
            }
        }

        // Libera os recursos de rede mantidos pelo canal.
        canal.shutdown();
        System.out.println("Encerrado.");
    }

    private static void consultarSaldo(PagamentoServiceGrpc.PagamentoServiceBlockingStub stub, Scanner entrada) {
        System.out.print("Conta: ");
        String conta = entrada.nextLine().trim();

        // A classe e seu builder foram gerados a partir de ConsultarSaldoRequest no .proto.
        ConsultarSaldoRequest request = ConsultarSaldoRequest.newBuilder().setConta(conta).build();

        // O stub serializa a requisição, realiza o RPC e desserializa a resposta.
        ConsultarSaldoResponse response = stub.consultarSaldo(request);

        System.out.println("Conta: " + response.getConta());
        System.out.println("Mensagem: " + response.getMensagem());
        if (response.getExiste()) {
            System.out.println("Saldo: " + formatarReais(response.getSaldoCentavos()));
        }
    }

    private static void realizarTransferencia(PagamentoServiceGrpc.PagamentoServiceBlockingStub stub,
                                              Scanner entrada) {
        System.out.print("Conta de origem: ");
        String origem = entrada.nextLine().trim();
        System.out.print("Conta de destino: ");
        String destino = entrada.nextLine().trim();
        System.out.print("Valor (ex.: 150.50): ");
        String valorTexto = entrada.nextLine().trim();

        long valorCentavos;
        try {
            valorCentavos = converterParaCentavos(valorTexto);
        } catch (NumberFormatException | ArithmeticException e) {
            System.out.println("Valor inválido: " + valorTexto);
            return;
        }

        ProcessarTransacaoRequest request = ProcessarTransacaoRequest.newBuilder()
                                                                     .setContaOrigem(origem)
                                                                     .setContaDestino(destino)
                                                                     .setValorCentavos(valorCentavos)
                                                                     .build();

        ProcessarTransacaoResponse response = stub.processarTransacao(request);

        System.out.println("Transação ID: " + response.getTransacaoId());
        System.out.println("Status: " + response.getStatus());
        System.out.println("Mensagem: " + response.getMensagem());
        System.out.println("Saldo da origem: " + formatarReais(response.getSaldoOrigemCentavos()));
    }

    // Aceita "150.50" ou "150,50" e converte para 15050 centavos sem usar ponto flutuante.
    private static long converterParaCentavos(String texto) {
        return new BigDecimal(texto.replace(",", ".")).movePointRight(2).longValueExact();
    }

    private static String formatarReais(long centavos) {
        return String.format("R$ %d,%02d", centavos / 100, Math.abs(centavos % 100));
    }
}
