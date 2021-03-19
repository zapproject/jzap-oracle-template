package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zapproject.jzap.EndpointParams;
import io.github.zapproject.jzap.InitCurve;
import io.github.zapproject.jzap.InitProvider;
import io.github.zapproject.jzap.NetworkProviderOptions;
import io.github.zapproject.jzap.Provider;
import io.github.zapproject.jzap.SetProviderTitle;
import io.github.zapproject.jzap.ZapToken;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthAccounts;
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

    public Oracle() throws Exception {
        map = new HashMap<String, Object>();
        ObjectMapper mapper = new ObjectMapper();
        map = (HashMap<String, Object>) mapper.readValue(new File("./Config.json"), new TypeReference<Map<String, Object>>(){});

        this.web3j = Web3j.build(new HttpService((String)map.get("NODE_URL")));
        this.creds = Credentials.create((String)map.get("account"));
        this.gasPro = new DefaultGasProvider();
    }

    public void validateConfig() {
        HashMap<String, Object> endpoint = (HashMap<String, Object>) map.get("EndpointSchema");
        assert map.get("title")!=null : "title is required to run Oracle";
        assert map.get("public_key")!=null : "public_key is required to run Oracle";
        assert endpoint.get("name")!=null : "Endpoint's name is required";
        assert endpoint.get("curve")!=null : "Curve is required for endpoint";
        assert endpoint.get("queryList")!=null : "Query list is recommende for date offer";
    }

    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        validateConfig();
        getProvider();
        Thread.sleep(5000);
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
                arg.title = ((String) map.get("title")).getBytes();
                TransactionReceipt ret = oracle.setTitle(arg);
            }
        }

        HashMap<String, Object> endpointSchema = (HashMap<String, Object>) map.get("EndpointSchema");
        boolean curveSet = oracle.isEndpointCreated(((String)endpointSchema.get("name")).getBytes());

        if (!curveSet) {
            System.out.println("No matching Endpoint found, creating endpoint");
            
            if ((String)endpointSchema.get("broker") == "") {
                endpointSchema.put("broker", "0x0000000000000000000000000000000000000000");
            }

            InitCurve init = new InitCurve();
            init.broker = (String)endpointSchema.get("broker");
            init.endpoint = ((String)endpointSchema.get("name")).getBytes();
            init.term = (List<BigInteger>)endpointSchema.get("curve");
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
                endpointParams.add(("Query string :" + query.get("query") +", Query params :" + params + ", Response Type: " + query.get("responseType")).getBytes());
            }
            System.out.println("Setting endpoint params");

            EndpointParams params = new EndpointParams();
            params.endpoint = ((String) map.get("name")).getBytes();
            params.endpointParams = endpointParams;
            TransactionReceipt txId = oracle.setEndpointParams(params);


        }
    }

    public void getProvider() throws Exception {
        EthAccounts accounts = web3j.ethAccounts().send();
        assert accounts.getAccounts().size() != 0 : "Unable to find an account in the current web3j provider, check your config variables";
        String owner = accounts.getAccounts().get(0);
        Credentials creds = Credentials.create(owner);
        ContractGasProvider gasPro = new DefaultGasProvider();
        NetworkProviderOptions options = new NetworkProviderOptions(31337, web3j, creds, gasPro);
        oracle = new Provider(options);

        token = ZapToken.load(options);
    }

    public void handleQuery(TransactionReceipt txReceipt) {
        
    }
}
