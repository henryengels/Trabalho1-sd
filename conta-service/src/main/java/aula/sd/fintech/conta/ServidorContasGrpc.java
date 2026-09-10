package aula.sd.fintech.conta;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

// Inicializa o servidor gRPC e publica a implementação do serviço de contas.
public class ServidorContasGrpc {

    private static final int PORTA = 9091;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server servidor = ServerBuilder.forPort(PORTA).addService(new ContaServiceImpl()).build().start();

        System.out.println("Servidor de contas gRPC ouvindo na porta " + PORTA);

        // Mantém o processo ativo enquanto o servidor espera novas chamadas.
        servidor.awaitTermination();
    }
}
