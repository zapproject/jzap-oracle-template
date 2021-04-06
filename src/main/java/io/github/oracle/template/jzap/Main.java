package io.github.oracle.template.jzap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;


public class Main {
	static Web3j web3j;
	static Credentials creds;
	static ContractGasProvider gasPro;

	public static void main(String[] args) throws Exception {
		String dir = new File("").getAbsolutePath();
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Object> map = (HashMap<String, Object>) mapper.readValue(new File(
                dir + "/src/main/java/io/github/oracle/template/jzap/Config.json"), 
                new TypeReference<Map<String, Object>>(){});
        
        if (((String)map.get("NODE_URL")).isEmpty())
            web3j = Web3j.build(new HttpService());
        else
            web3j = Web3j.build(new HttpService((String)map.get("NODE_URL")));
        creds = Credentials.create((String)map.get("account"));
        gasPro = new DefaultGasProvider();

		Oracle oracle = new Oracle(web3j, creds, gasPro);
		oracle.start();
		
		// Thread.sleep(2000);
		// Subscribe subscribe = new Subscribe(web3j, creds, gasPro);
		// subscribe.start();
		
	}
}
