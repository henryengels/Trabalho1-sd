package aula.sd.fintech.conta;

import aula.sd.fintech.grpc.ConsultarSaldoRequest;
import aula.sd.fintech.grpc.ConsultarSaldoResponse;
import aula.sd.fintech.grpc.ContaServiceGrpc;
import aula.sd.fintech.grpc.TransferirRequest;
import aula.sd.fintech.grpc.TransferirResponse;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

// Implementa no servidor o contrato gerado a partir de ContaService no .proto.
// Guarda os saldos em memória e é o único ponto que altera as contas.
public class ContaServiceImpl extends ContaServiceGrpc.ContaServiceImplBase {

    // Saldos iniciais em reais: ana = R$ 1.000,00, bruno = R$ 500,00, carla = R$ 0,00.
    private final Map<String, Long> saldos = new HashMap<>(Map.of(
            "ana", 100_000L,
            "bruno", 50_000L,
            "carla", 0L,
            "savio", 1000000_000L,
            "henry", 100_050L,
            "matheus", 50000_000L,
            "paulo", 1000_000L
    ));

    @Override
    public void consultarSaldo(ConsultarSaldoRequest request,
                               StreamObserver<ConsultarSaldoResponse> responseObserver) {
        String conta = request.getConta();
        Long saldo;
        synchronized (saldos) {
            saldo = saldos.get(conta);
        }
        boolean existe = saldo != null;

        System.out.printf("Consulta de saldo: conta=%s, existe=%s%n", conta, existe);

        ConsultarSaldoResponse response = ConsultarSaldoResponse.newBuilder()
                                                                .setConta(conta)
                                                                .setExiste(existe)
                                                                .setSaldoCentavos(existe ? saldo : 0L)
                                                                .setMensagem(existe
                                                                             ? "Conta encontrada."
                                                                             : "Conta não encontrada.")
                                                                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void transferir(TransferirRequest request, StreamObserver<TransferirResponse> responseObserver) {
        String origem = request.getContaOrigem();
        String destino = request.getContaDestino();
        long valor = request.getValorCentavos();

        System.out.printf("Transferência recebida: origem=%s, destino=%s, valor=%s%n",
                          origem, destino, formatarReais(valor));

        TransferirResponse.Builder resposta = TransferirResponse.newBuilder();

        // O bloco sincronizado garante que a validação e o débito/crédito aconteçam
        // juntos, mesmo com várias transações chegando ao mesmo tempo.
        synchronized (saldos) {
            String motivoRecusa = validar(origem, destino, valor);

            if (motivoRecusa != null) {
                resposta.setAprovada(false)
                        .setMensagem(motivoRecusa)
                        .setSaldoOrigemCentavos(saldos.getOrDefault(origem, 0L));
            } else {
                saldos.put(origem, saldos.get(origem) - valor);
                saldos.put(destino, saldos.get(destino) + valor);
                resposta.setAprovada(true)
                        .setMensagem("Transferência efetivada.")
                        .setSaldoOrigemCentavos(saldos.get(origem));
            }
        }

        System.out.printf("Resultado: aprovada=%s, mensagem=%s%n", resposta.getAprovada(), resposta.getMensagem());

        responseObserver.onNext(resposta.build());
        responseObserver.onCompleted();
    }

    // Devolve o motivo da recusa ou null quando a transferência pode ser feita.
    private String validar(String origem, String destino, long valor) {
        if (valor <= 0) {
            return "Valor deve ser maior que zero.";
        }
        if (origem.equals(destino)) {
            return "Conta de origem e destino devem ser diferentes.";
        }
        if (!saldos.containsKey(origem)) {
            return "Conta de origem não encontrada.";
        }
        if (!saldos.containsKey(destino)) {
            return "Conta de destino não encontrada.";
        }
        if (saldos.get(origem) < valor) {
            return "Saldo insuficiente.";
        }
        return null;
    }

    private static String formatarReais(long centavos) {
        return String.format("R$ %d,%02d", centavos / 100, Math.abs(centavos % 100));
    }
}
