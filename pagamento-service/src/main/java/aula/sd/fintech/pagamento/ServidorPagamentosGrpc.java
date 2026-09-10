package aula.sd.fintech.pagamento;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// Inicializa o servidor gRPC e publica a implementação do serviço de pagamentos.
public class ServidorPagamentosGrpc {

    private static final int PORTA = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Registra o serviço que atenderá os métodos definidos no arquivo .proto.
        Server servidor = ServerBuilder.forPort(PORTA)
                                        .addService(new PagamentoServiceImpl())
                                        .build()
                                        .start();

        System.out.println("Servidor de pagamentos gRPC ouvindo na porta " + PORTA);

        // Mantém o processo ativo enquanto o servidor espera novas chamadas.
        servidor.awaitTermination();
    }
}
