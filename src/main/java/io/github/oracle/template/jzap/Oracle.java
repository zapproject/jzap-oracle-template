package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zapproject.jzap.Dispatch.IncomingEventResponse;
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
import io.reactivex.Flowable;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;



public class Oracle extends Thread {
    public Provider oracle;
    public ZapToken token;
    public Web3j web3j;
    public Credentials creds;
    public ContractGasProvider gasPro;
    public HashMap<String, Object> map;
    public Responder responder;
    
    private IPFS ipfs;
    private final String IPFS_GATEWAY = "https://gateway.ipfs.io/ipfs/";

    public Oracle(Web3j web3j, Credentials creds, ContractGasProvider gasPro) throws Exception {
        String dir = new File("").getAbsolutePath();
        ObjectMapper mapper = new ObjectMapper();
        map = (HashMap<String, Object>) mapper.readValue(new File(
                dir + "/src/main/java/io/github/oracle/template/jzap/Config.json"), 
                new TypeReference<Map<String, Object>>(){});
        
        // if (((String)map.get("NODE_URL")).isEmpty())
        //     this.web3j = Web3j.build(new HttpService());
        // else
        //     this.web3j = Web3j.build(new HttpService((String)map.get("NODE_URL")));
        // this.creds = Credentials.create((String)map.get("account"));
        // this.gasPro = new DefaultGasProvider();
        this.web3j = web3j;
        this.creds = creds;
        this.gasPro = gasPro;
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

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        validateConfig();

        try {
            getProvider();
        } catch (Exception e ) {
            System.out.println("Issue with provider");
        }

        byte[] title;
        
        try {
            title = oracle.getTitle();
        } catch (Exception e) {
            title = new byte[32];
        }
        
        if (new String(title, StandardCharsets.UTF_8).trim().isBlank()) {
            System.out.println("No provider found, Initializing provider");
            InitProvider init = new InitProvider();
            init.publicKey = BigInteger.valueOf((int)map.get("public_key"));
            title = new byte[32];
            System.arraycopy(((String) map.get("title")).getBytes(), 0, title, 0, ((String) map.get("title")).getBytes().length);
            init.title = title;

            try {
                TransactionReceipt res = oracle.initiateProvider(init);
            } catch (Exception e) {
                System.out.println("Issue with initiating Provider");
            }
        } else {
            System.out.println("Oracle exists");
            if (title != map.get("title")) {
                System.out.println("Changing title");
                SetProviderTitle arg = new SetProviderTitle();
                title = new byte[32];
                System.arraycopy(((String) map.get("title")).getBytes(), 0, title, 0, ((String) map.get("title")).getBytes().length);
                arg.title = title;

                try {
                    TransactionReceipt ret = oracle.setTitle(arg);
                } catch (Exception e) {
                    System.out.println("Issue with setting the provider title");
                }
            }
        }

        HashMap<String, Object> endpointSchema = (HashMap<String, Object>) map.get("EndpointSchema");
        byte[] name = new byte[32];
        System.arraycopy(((String)endpointSchema.get("name")).getBytes(), 0, name, 0, ((String)endpointSchema.get("name")).getBytes().length);
  
        // boolean curveSet = oracle.isEndpointCreated(name);
        boolean curveNotSet = true;
        try {
            curveNotSet = oracle.registry.getCurveUnset(creds.getAddress(), name).send();
        } catch (Exception e) {
            System.out.println("Issue with checking if curve exists");
        }

        if (curveNotSet) {
            System.out.println("No matching Endpoint found, creating endpoint");
            
            if ((String)endpointSchema.get("broker") == "") {
                endpointSchema.put("broker", "0x0000000000000000000000000000000000000000");
            }

            InitCurve init = new InitCurve();
            init.broker = (String)endpointSchema.get("broker");
            init.endpoint = name;
            init.term = new ArrayList<BigInteger>();
    
            for (Integer point : (List<Integer>) endpointSchema.get("curve")) {
                init.term.add(BigInteger.valueOf(point));
            }
            try {
                TransactionReceipt createEndpoint = oracle.initiateProviderCurve(init);
            } catch (Exception e) {
                System.out.println("Issue with initiating curve");
            }
            
            System.out.println("Successfully created endpoint");
            List<byte[]> endpointParams = new ArrayList<byte[]>();
            List<Object> queryList = (ArrayList<Object>)endpointSchema.get("queryList");
        
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
            params.endpoint = name;
            params.endpointParams = endpointParams;

            try {
                TransactionReceipt txId = oracle.setEndpointParams(params);
            } catch (Exception e) {
                System.out.println("Issue with setting endpoint params");
            }

            Map<String, Object> mapJson = new HashMap<>();
            mapJson.put("name", endpointSchema.get("name"));
            mapJson.put("curve", endpointSchema.get("curve"));
            mapJson.put("broker", endpointSchema.get("broker"));
            mapJson.put("params", endpointParams);

            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.writeValue(Paths.get("endpointSchema.json").toFile(), mapJson);
            } catch (Exception e) {
                System.out.println("Issue writing engpointSchema JSON file");
            }
            
            System.out.println("Saving endpoint info into ipfs");
            NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File("endpointSchema.json"));
            List<MerkleNode> node = new ArrayList<MerkleNode>();

            try {
                node = ipfs.add(file);
            } catch (Exception e) {
                System.out.println("Issue with adding JSON to IPFS");
            }
            

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

            try {
                oracle.setProviderParameter(setParams);
            } catch (Exception e) {
                System.out.println("Issue with setting provider params");
            }
            
        } else {
            System.out.println("curve is already set");
        }
        System.out.println("FINDING");
        // Thread.sleep(20000);
        while (true) {
            // try {
            //     Subscribe subscribe = new Subscribe(web3j, creds, gasPro);
            //     subscribe.run();
            //     Thread.sleep(5000);
            // } catch (Exception e) {
            //     // TODO Auto-generated catch block
            //     e.printStackTrace();
            // }
            
            System.out.println("Listening for query");
            try { 
                Flowable<IncomingEventResponse> flow = oracle.dispatch.incomingEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST);

                flow
                    // .subscribeOn(Schedulers.newThread())
                    // .observeOn(Schedulers.newThread())
                    // .onErrorResumeNext(tx -> {})
                    // .retry(60)
                    .subscribe(tx -> {
                        handleQuery(tx);
                    });

            } catch (Exception ne) {

            }
        }        
    }

    public void getProvider() throws Exception {
        ContractGasProvider gasPro = new DefaultGasProvider();
        NetworkProviderOptions options = new NetworkProviderOptions(31337, web3j, creds, gasPro);
        oracle = new Provider(options);

        token = ZapToken.load(options);
    }

    @SuppressWarnings("unchecked")
    public void handleQuery(IncomingEventResponse event) {
        byte[] endpoint = new byte[32];
        byte[] configEP = ((String)((HashMap<String, Object>) map.get("EndpointSchema")).get("name")).getBytes();
        System.arraycopy(configEP, 0, endpoint, 0, configEP.length);
        
        if (!Arrays.equals(event.endpoint, endpoint)) {
            System.out.println("Unable to find the callback for " + event.endpoint);
            return;
        }

        List<String> params = new ArrayList<String>();
        Bytes32 from = ((List<Bytes32>)((Object)event.endpointParams)).get(0);
        params.add(new String(from.getValue(), StandardCharsets.UTF_8));

        System.out.println("Received query to " + event.endpoint + " from " + 
        event.onchainSubscriber + " at address " + event.subscriber);

        System.out.println("Query ID " + event.id + "...: " + event.query + 
                ". Parameters: " + event.endpointParams.toString());

        // try {
        //     Thread.sleep(5000);
        // } catch (Exception e) {
            
        // }
        
        
        for (Map<String, Object> query : (List<Map<String,Object>>)((Map<String, Object>) map.get("EndpointSchema")).get("queryList")) {
            try {
                String response = responder.getResponse(event.query, "USD", 7);
                System.out.println("got response from getResponse method : " + response);
                List<String> param = new ArrayList<String>();
                param.add(response);

                System.out.println("Responding to offchain subscriber");
                ResponseArgs args = new ResponseArgs();
                args.queryID = event.id;
                args.responseParams = param;
                args.dynamic = (Boolean)query.get("dynamic");
                TransactionReceipt tx = oracle.respond(args);

                System.out.println("Responded to " + event.subscriber + " in transaction " + tx.getTransactionHash());
            } catch (Exception e) {
                throw new Error("Error Responding to query " + event.id + " : " + e.getMessage());
            }
        }
    }
}
