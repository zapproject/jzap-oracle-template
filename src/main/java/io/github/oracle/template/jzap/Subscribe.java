package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zapproject.jzap.ApproveType;
import io.github.zapproject.jzap.BondType;
import io.github.zapproject.jzap.DelegateBondType;
import io.github.zapproject.jzap.Dispatch.OffchainResult1EventResponse;
import io.github.zapproject.jzap.NetworkProviderOptions;
import io.github.zapproject.jzap.QueryArgs;
import io.github.zapproject.jzap.SubscribeType;
import io.github.zapproject.jzap.Subscriber;
import io.reactivex.Flowable;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.gas.ContractGasProvider;


public class Subscribe extends Thread {
    Subscriber subscriber;
    Web3j web3j;
    Credentials creds;
    Credentials oracle;
    ContractGasProvider gasPro;

    HashMap<String, Object> map;
    String approveDots = "1000000000";
    byte[] endpoint = new byte[32];
    static BigInteger lastResponse = new BigInteger("0");

    public Subscribe(Web3j web3j, Credentials creds, ContractGasProvider gasPro) throws Exception {
        String dir = new File("").getAbsolutePath();
        ObjectMapper mapper = new ObjectMapper();
        map = (HashMap<String, Object>) mapper.readValue(new File(
                dir + "/src/main/java/io/github/oracle/template/jzap/Config.json"), 
                new TypeReference<Map<String, Object>>(){});
        
        this.web3j = web3j;
        this.creds = Credentials.create("0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a");
        this.gasPro = gasPro;
        this.oracle = creds;
        System.arraycopy("Zap Price".getBytes(), 0, endpoint, 0, 9);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        System.out.println("Creating a query");
        try {
            getSubscriber();
        } catch (Exception e) {
            System.out.println("Issue with loading subscriber");
        }
        

        // Approve Dots
        ApproveType atype = new ApproveType();
        atype.provider = creds.getAddress();
        atype.zapNum = new BigInteger(approveDots);
        try {
            subscriber.approveToBond(atype);
        } catch (Exception e) {
            System.out.println("Issue with approving bond");
        }
        

        // Delegate Dots
        DelegateBondType dtype = new DelegateBondType();
        dtype.dots = new BigInteger("100");
        dtype.endpoint = endpoint;
        dtype.provider = oracle.getAddress();
        dtype.subscriber = subscriber.bondage.address;
        try {
            subscriber.delegateBond(dtype);
        } catch (Exception e) {
            System.out.println("Issue with delegating bond");
        }

        // Bond
        BondType btype = new BondType();
        btype.dots = new BigInteger("100");
        btype.endpoint = endpoint;
        btype.provider = oracle.getAddress();
        btype.subscriber = subscriber.bondage.address;
        try {
            subscriber.bond(btype);
        } catch (Exception e) {
            System.out.println("Issue with bonding");
        }
        
        // initiateSubscriber
        SubscribeType stype = new SubscribeType();
        stype.dots = new BigInteger("10");
        stype.endpoint = endpoint;
        HashMap<String, Object> endpointSchema = (HashMap<String, Object>) map.get("EndpointSchema");
        List<byte[]> endpointParams = new ArrayList<byte[]>();
        List<Object> queryLis = (ArrayList<Object>)endpointSchema.get("queryList");
    
        for (int i=0;i<queryLis.size();i++) {
            HashMap<String, Object> query = (HashMap<String, Object>)queryLis.get(i);
            List<String> queryParams = (ArrayList<String>) query.get("params");
            String params = "";
            for (String param : queryParams) {
                params+=param;
            }
            // not necessary
            byte[] temp = ("Query string :" + query.get("query") +", Query params :" + params + ", Response Type: " + query.get("responseType")).getBytes();
            byte[] param = new byte[32];
            if (temp.length > 32)
                System.arraycopy(temp, 0, param, 0, 32);
            else
                System.arraycopy(temp, 0, param, 0, temp.length);

            endpointParams.add(param);
        }
        // stype.endpointParams = endpointParams;
        // stype.provider = oracle.getAddress();
        // try {
        //     subscriber.subscribe(stype);
        // } catch (Exception e1) {
        //     // TODO Auto-generated catch block
        //     e1.printStackTrace();
        // }

        // Make Query
        QueryArgs args = new QueryArgs();
        args.endpoint = endpoint;
        // args.endpointParams = new ArrayList<byte[]>();
        // byte[] params = new byte[32];
        // System.arraycopy("int".getBytes(), 0, params, 0, 3);
        // args.endpointParams.add(params);
        args.endpointParams = endpointParams;
        args.provider = oracle.getAddress();

        HashMap<String, Object> schema = (HashMap<String, Object>) map.get("EndpointSchema");
        List<Object> queryList = (ArrayList<Object>)schema.get("queryList");
        
        for (int i=0;i<queryList.size();i++) {
            HashMap<String, Object> query = (HashMap<String, Object>)queryList.get(i);

            args.query = (String) query.get("query");
            System.out.println("Sending query");
            try {
                subscriber.queryData(args);
            } catch (Exception e) {
                System.out.println("Issue with creating a query");
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        while (true) {
            try {
                // Limit to one per sec
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Flowable<OffchainResult1EventResponse> flow = subscriber.dispatch.offchainResult1EventFlowable(DefaultBlockParameter.valueOf(lastResponse), DefaultBlockParameterName.LATEST);
            flow
                .onErrorResumeNext(tx -> {})
                .subscribe(tx -> {
                    handleReponse(tx);
                });
        }
    }

    public void getSubscriber() throws Exception {
        NetworkProviderOptions options = new NetworkProviderOptions(31337, web3j, creds, gasPro);
        subscriber = new Subscriber(options);
    }

    public void handleReponse(OffchainResult1EventResponse event) {
        lastResponse = event.log.getBlockNumber().add(BigInteger.valueOf(1));
        System.out.println("Getting response event: " + event.response1);
        
        // cancel query
        // try {
        //     subscriber.cancelQuery(event.id);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }
}
