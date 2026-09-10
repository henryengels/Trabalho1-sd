package aula.sd.fintech.pagamento;

import aula.sd.fintech.grpc.ConsultarSaldoRequest;
import aula.sd.fintech.grpc.ConsultarSaldoResponse;
import aula.sd.fintech.grpc.ContaServiceGrpc;
import aula.sd.fintech.grpc.TransferirRequest;
import aula.sd.fintech.grpc.TransferirResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Cliente interno usado pelo serviço de pagamentos para falar com o serviço de contas.
public class ClienteContasGrpc {

    private final ContaServiceGrpc.ContaServiceBlockingStub stub;

    public ClienteContasGrpc(String host, int porta) {
        ManagedChannel canal = ManagedChannelBuilder.forAddress(host, porta).usePlaintext().build();
        stub = ContaServiceGrpc.newBlockingStub(canal);
    }

    public ConsultarSaldoResponse consultarSaldo(String conta) {
        ConsultarSaldoRequest request = ConsultarSaldoRequest.newBuilder().setConta(conta).build();
        return stub.consultarSaldo(request);
    }

    public TransferirResponse transferir(String origem, String destino, long valorCentavos) {
        TransferirRequest request = TransferirRequest.newBuilder()
                                                     .setContaOrigem(origem)
                                                     .setContaDestino(destino)
                                                     .setValorCentavos(valorCentavos)
                                                     .build();
        return stub.transferir(request);
    }
}
