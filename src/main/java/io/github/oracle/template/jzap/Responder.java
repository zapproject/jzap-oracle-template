package io.github.oracle.template.jzap;

import io.github.zapproject.jzap.wrappers.Registry;
import java.math.BigInteger;
import java.util.List;


public class Responder {

    public Responder() {
    }

    public String getResponse(Registry registry, int days, int hours) {
        return "";
    }

    public AOracle getHighest(Registry registry) throws Exception {
        Curve curves;
        BigInteger high = BigInteger.valueOf(0);
        List<String> oracleAddresses = (List<String>) registry.getAllOracles().send();
        
        for (String address : oracleAddresses) {
            List<byte[]> endpoints = registry.getProviderEndpoints(address).send();
            
            for (byte[] endpoint : endpoints) {
                List<BigInteger> curve = registry.getProviderCurve(address, endpoint).send();
                curves = new Curve(curve);
            }
        }


        return new AOracle();
    }
}

class AOracle {
    String address;
    String endpoint;
    List<BigInteger> curve;
    BigInteger yeild;
}
