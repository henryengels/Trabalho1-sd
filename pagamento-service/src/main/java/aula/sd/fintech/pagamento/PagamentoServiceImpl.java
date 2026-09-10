package aula.sd.fintech.pagamento;

import aula.sd.fintech.grpc.ConsultarSaldoRequest;
import aula.sd.fintech.grpc.ConsultarSaldoResponse;
import aula.sd.fintech.grpc.PagamentoServiceGrpc;
import aula.sd.fintech.grpc.ProcessarTransacaoRequest;
import aula.sd.fintech.grpc.ProcessarTransacaoResponse;
import aula.sd.fintech.grpc.TransferirResponse;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

// Implementa no servidor o contrato gerado a partir de PagamentoService no .proto.
// Recebe a solicitação do cliente e delega a validação e o débito ao serviço de contas.
public class PagamentoServiceImpl extends PagamentoServiceGrpc.PagamentoServiceImplBase {

    private static final String CONTAS_HOST = "localhost";
    private static final int CONTAS_PORTA = 9091;

    private final ClienteContasGrpc clienteContas = new ClienteContasGrpc(CONTAS_HOST, CONTAS_PORTA);

    @Override
    // request é a única entrada declarada no .proto. O gRPC adiciona responseObserver
    // à API Java do servidor para enviar a resposta; por isso o método retorna void.
    public void processarTransacao(ProcessarTransacaoRequest request,
                                   StreamObserver<ProcessarTransacaoResponse> responseObserver) {
        String transacaoId = UUID.randomUUID().toString();

        // A requisição já chega desserializada como uma mensagem Protobuf.
        System.out.printf("Transação %s recebida: origem=%s, destino=%s, valor=%s%n",
                          transacaoId, request.getContaOrigem(), request.getContaDestino(),
                          formatarReais(request.getValorCentavos()));

        // O serviço de contas valida saldo e contas e efetiva o débito/crédito.
        TransferirResponse resultado = clienteContas.transferir(request.getContaOrigem(),
                                                                request.getContaDestino(),
                                                                request.getValorCentavos());

        String status = resultado.getAprovada() ? "APROVADA" : "RECUSADA";
        System.out.printf("Transação %s %s: %s%n", transacaoId, status, resultado.getMensagem());

        // A resposta também é uma mensagem imutável gerada pelo Protobuf.
        ProcessarTransacaoResponse response = ProcessarTransacaoResponse.newBuilder()
                                                                        .setTransacaoId(transacaoId)
                                                                        .setStatus(status)
                                                                        .setMensagem(resultado.getMensagem())
                                                                        .setSaldoOrigemCentavos(resultado.getSaldoOrigemCentavos())
                                                                        .build();

        // Envia a resposta ao cliente e sinaliza que o RPC terminou com sucesso.
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void consultarSaldo(ConsultarSaldoRequest request,
                               StreamObserver<ConsultarSaldoResponse> responseObserver) {
        System.out.printf("Consulta de saldo recebida: conta=%s%n", request.getConta());

        // Apenas repassa a consulta ao serviço de contas e devolve a mesma resposta.
        ConsultarSaldoResponse response = clienteContas.consultarSaldo(request.getConta());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private static String formatarReais(long centavos) {
        return String.format("R$ %d,%02d", centavos / 100, Math.abs(centavos % 100));
    }
}
