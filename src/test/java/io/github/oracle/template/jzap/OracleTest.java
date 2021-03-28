package io.github.oracle.template.jzap;

import org.junit.jupiter.api.Test;


class OracleTest {
    // @Test
    // void test() throws Exception {
    //     String dir = new File("").getAbsolutePath();
    //     ObjectMapper mapper = new ObjectMapper();
    //     HashMap<String, Object> map = (HashMap<String, Object>) mapper.readValue(
    //         new File(dir+"/src/main/java/io/github/oracle/template/jzap/Config.json"), 
    //         new TypeReference<Map<String, Object>>(){});
    //     // System.out.println(map);

    //     Web3j.build(new HttpService((String)map.get("NODE_URL")));
    // }
    @Test
    void InitializeTest() throws Exception {
        Oracle oracle = new Oracle();
        // System.out.println("############");
        oracle.initialize();
    }
}