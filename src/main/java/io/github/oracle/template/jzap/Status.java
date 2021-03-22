package io.github.oracle.template.jzap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthSign;


@RestController
public class Status {

    public void updateStatus(Web3j web3j, String oracle, String endpoint) throws InterruptedException, IOException {
        while (true) {
            update(web3j, oracle, endpoint);
            Thread.sleep(3 * 6 * 1000);
        }
    }

    public void update(Web3j web3j, String oracle, String endpoint) throws IOException {
        String data = endpoint+new java.util.Date();
        EthSign signature = web3j.ethSign(oracle, data).send();
        send(data, signature);
    }

    @PostMapping(path = "/update")
    public void send(@RequestBody String data, @RequestBody EthSign signature) {

    }

    public void connectStatus(Web3j web3j, String endpoint) throws Exception {
        EthAccounts accounts = web3j.ethAccounts().send();
        String oracle = accounts.getAccounts().get(0);
        Socket socket = IO.socket("http://localhost:8000");
        socket.on("connect", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    EthSign signature = web3j.ethSign(endpoint, oracle).send();
                    System.out.println(signature);
                    String sig = mapper.writeValueAsString(signature);
                    sig = "{\"endpoint\":\"TrendSignals\"," + sig.substring(1);
                    socket.emit("authentication", sig);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        socket.on("authenticated", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("authenticated");
            }
            
        });

        socket.on("unauthorized", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("unauthorized");
            }

        });
    }
}
