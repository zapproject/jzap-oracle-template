package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.zapproject.jzap.ApproveType;
import io.github.zapproject.jzap.BondType;
import io.github.zapproject.jzap.DelegateBondType;
import io.github.zapproject.jzap.Dispatch.OffchainResult1EventResponse;
import io.github.zapproject.jzap.NetworkProviderOptions;
import io.github.zapproject.jzap.QueryArgs;
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
        this.creds = creds;
        this.gasPro = gasPro;
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
        dtype.provider = creds.getAddress();
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
        btype.provider = creds.getAddress();
        btype.subscriber = subscriber.bondage.address;
        try {
            subscriber.bond(btype);
        } catch (Exception e) {
            System.out.println("Issue with bonding");
        }
        
        // Make Query
        QueryArgs args = new QueryArgs();
        args.endpoint = endpoint;
        args.endpointParams = new ArrayList<byte[]>();
        byte[] params = new byte[32];
        System.arraycopy("int".getBytes(), 0, params, 0, 3);
        args.endpointParams.add(params);
        args.provider = creds.getAddress();

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        while (true) {
            try {
                // Limit to one per sec
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
