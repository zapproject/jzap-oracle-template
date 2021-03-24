package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zapproject.jzap.Dispatch;
import io.github.zapproject.jzap.EndpointParams;
import io.github.zapproject.jzap.InitCurve;
import io.github.zapproject.jzap.InitProvider;
import io.github.zapproject.jzap.NetworkProviderOptions;
import io.github.zapproject.jzap.Provider;
import io.github.zapproject.jzap.ResponseArgs;
import io.github.zapproject.jzap.SetProviderParams;
import io.github.zapproject.jzap.SetProviderTitle;
import io.github.zapproject.jzap.ZapToken;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import java.io.File;
import java.lang.Integer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;


public class Oracle {
    public Provider oracle;
    public ZapToken token;
    public Web3j web3j;
    public Credentials creds;
    public ContractGasProvider gasPro;
    public HashMap<String, Object> map;
    public Responder responder;
    
    private IPFS ipfs;
    private final String IPFS_GATEWAY = "https://gateway.ipfs.io/ipfs/";

    public Oracle() throws Exception {
        String dir = new File("").getAbsolutePath();
        ObjectMapper mapper = new ObjectMapper();
        map = (HashMap<String, Object>) mapper.readValue(new File(
                dir + "/src/main/java/io/github/oracle/template/jzap/Config.json"), 
                new TypeReference<Map<String, Object>>(){});
        
        if (((String)map.get("NODE_URL")).isEmpty())
            this.web3j = Web3j.build(new HttpService());
        else
            this.web3j = Web3j.build(new HttpService((String)map.get("NODE_URL")));
        this.creds = Credentials.create((String)map.get("account"));
        this.gasPro = new DefaultGasProvider();
        this.responder = new Responder();
        this.ipfs = new IPFS("/dnsaddr/ipfs.infura.io/tcp/5001/https");
    }

    public void validateConfig() {
        HashMap<String, Object> endpoint = (HashMap<String, Object>) map.get("EndpointSchema");
        assert map.get("title")!=null : "title is required to run Oracle";
        assert map.get("public_key")!=null : "public_key is required to run Oracle";
        assert endpoint.get("name")!=null : "Endpoint's name is required";
        assert endpoint.get("curve")!=null : "Curve is required for endpoint";
        assert endpoint.get("queryList")!=null : "Query list is recommended for date offer";
    }

    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        validateConfig();
        getProvider();
        // Thread.sleep(5);
        byte[] title = oracle.getTitle();
        if (title.length == 0) {
            System.out.println("No provider found, Initializing provider");
            InitProvider init = new InitProvider();
            init.publicKey = BigInteger.valueOf((int)map.get("public_key"));
            init.title = ((String) map.get("title")).getBytes();
            TransactionReceipt res = oracle.initiateProvider(init);
        } else {
            System.out.println("Oracle exists");
            if (title != map.get("title")) {
                System.out.println("Changing title");
                SetProviderTitle arg = new SetProviderTitle();
                title = new byte[32];
                System.arraycopy(((String) map.get("title")).getBytes(), 0, title, 0, ((String) map.get("title")).getBytes().length);
                arg.title = title;
                TransactionReceipt ret = oracle.setTitle(arg);
            }
        }

        HashMap<String, Object> endpointSchema = (HashMap<String, Object>) map.get("EndpointSchema");
        title = new byte[32];
        System.arraycopy(((String)endpointSchema.get("name")).getBytes(), 0, title, 0, ((String)endpointSchema.get("name")).getBytes().length);
        boolean curveSet = oracle.isEndpointCreated(title);

        if (curveSet) {
            System.out.println("No matching Endpoint found, creating endpoint");
            
            if ((String)endpointSchema.get("broker") == "") {
                endpointSchema.put("broker", "0x0000000000000000000000000000000000000000");
            }

            // title = new byte[32];
            // System.arraycopy(((String)endpointSchema.get("name")).getBytes(), 0, title, 0, ((String)endpointSchema.get("name")).getBytes().length);
            InitCurve init = new InitCurve();
            init.broker = (String)endpointSchema.get("broker");
            init.endpoint = title;
            init.term = new ArrayList<BigInteger>();
    
            for (Integer point : (List<Integer>) endpointSchema.get("curve")) {
                init.term.add(BigInteger.valueOf(point));
            }
            
            TransactionReceipt createEndpoint = oracle.initiateProviderCurve(init);
            System.out.println("Successfully created endpoint");

            List<byte[]> endpointParams = new ArrayList<byte[]>();
            List queryList = (ArrayList<Object>)endpointSchema.get("queryList");
        
            String endParams;
            for (int i=0;i<queryList.size();i++) {
                HashMap<String, Object> query = (HashMap<String, Object>)queryList.get(i);
                List<String> queryParams = (ArrayList<String>) query.get("params");
                String params = "";
                for (String param : queryParams) {
                    params+=param;
                }
                
                byte[] temp = ("Query string :" + query.get("query") +", Query params :" + params + ", Response Type: " + query.get("responseType")).getBytes();
                byte[] param = new byte[32];
                if (temp.length > 32)
                    System.arraycopy(temp, 0, param, 0, 32);
                else
                    System.arraycopy(temp, 0, param, 0, temp.length);

                endpointParams.add(param);
            }

            System.out.println("Setting endpoint params");

            EndpointParams params = new EndpointParams();
            // params.endpoint = ((String) map.get("name")).getBytes();
            params.endpoint = title;
            params.endpointParams = endpointParams;
            TransactionReceipt txId = oracle.setEndpointParams(params);

            Map<String, Object> mapJson = new HashMap<>();
            map.put("name", endpointSchema.get("name"));
            map.put("curve", endpointSchema.get("curve"));
            map.put("broker", endpointSchema.get("broker"));
            map.put("params", endpointParams);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("endpointSchema.json").toFile(), map);
            System.out.println("Saving endpoint info into ipfs");
            NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File("endpointSchema.json"));
            List<MerkleNode> node = ipfs.add(file);

            SetProviderParams setParams = new SetProviderParams();
            setParams.key = new byte[32];
            if (((String) endpointSchema.get("name")).getBytes().length > 32)
                System.arraycopy(((String) endpointSchema.get("name")).getBytes(), 0, setParams.key, 0, 32);
            else
                System.arraycopy(((String) endpointSchema.get("name")).getBytes(), 0, setParams.key, 0, ( (String) endpointSchema.get("name")).getBytes().length);
            setParams.value = new byte[32];
            if ((IPFS_GATEWAY + node.get(0).hash).getBytes().length > 32 )
                System.arraycopy((IPFS_GATEWAY + node.get(0).hash).getBytes(), 0, setParams.value, 0, 32);
            else
                System.arraycopy((IPFS_GATEWAY + node.get(0).hash).getBytes(), 0, setParams.value, 0, (IPFS_GATEWAY + node.get(0).hash).getBytes().length);

            oracle.setProviderParameter(setParams);
        } else {
            System.out.println("curve is already set");
        }

        while (true) {
            try { 
                oracle.dispatch.incomingEventFlowable(DefaultBlockParameterName.EARLIEST,
                    DefaultBlockParameterName.LATEST).subscribe(tx -> {
                        handleQuery(tx);
                    });
            } catch (NullPointerException ne) {

            }
        }        
    }

    public void getProvider() throws Exception {
        // EthAccounts accounts = web3j.ethAccounts().send();
        // assert accounts.getAccounts().size() != 0 : "Unable to find an account in the current web3j provider, check your config variables";
        // String owner = accounts.getAccounts().get(0);
        // Credentials creds = Credentials.create(owner);
        ContractGasProvider gasPro = new DefaultGasProvider();
        NetworkProviderOptions options = new NetworkProviderOptions(31337, web3j, creds, gasPro);
        oracle = new Provider(options);

        token = ZapToken.load(options);
    }

    @SuppressWarnings("unchecked")
    public void handleQuery(Dispatch.IncomingEventResponse event) {
        if (Arrays.equals(event.endpoint, ((String) map.get("name")).getBytes())) {
            System.out.println("Unable to find the callback for " + event.endpoint);
            return;
        }

        String endpointParams = "";
        List<String> params = new ArrayList<String>();

        for (byte[] param : event.endpointParams) {
            params.add(new String(param, StandardCharsets.UTF_8));
            endpointParams += params.get(params.size()-1);
        }

        System.out.println("Received query to " + event.endpoint + " from " + 
        event.onchainSubscriber + " at address " + event.subscriber);

        System.out.println("Query ID " + event.id + "...: " + event.query + 
                ". Parameters: " + event.endpointParams);

        for (Map<String, Object> query : (List<Map<String, Object>>) map.get("queryList")) {
            try {
                String response = responder.getResponse(event.query, endpointParams);
                System.out.println("got response from getResponse method : " + response);

                System.out.println("Responding to offchain subscriber");
                ResponseArgs args = new ResponseArgs();
                args.queryID = event.id;
                args.responseParams = params;
                args.dynamic = (Boolean)query.get("dynamic");
                TransactionReceipt tx = oracle.respond(args);

                System.out.println("Responded to " + event.subscriber + " in transaction " + tx.getTransactionHash());
            } catch (Exception e) {
                throw new Error("Error Responding to query " + event.id + " : " + e.getMessage());
            }
        }
    }
}
