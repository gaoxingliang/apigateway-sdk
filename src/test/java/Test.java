import cn.sichuancredit.apigateway.client.*;

import java.util.*;

public class Test {
    public static void main(String[] args) {
        ApiConfig config = new ApiConfig();
        config.setUsername("usernameXXX");
        config.setPassword("passwordXXX");
        config.setUrl("http://devicbc.sichuancredit.cn:88");
        config.setPrivateKey("yourPrivateKey");
        config.setPublicKey("yourPublicKey");

        ApiClient apiClient = new ApiClient(config);
        Map<String, Object> params = new HashMap<>();
        params.put("enterprise", "四川征信有限公司");
        System.out.println(apiClient.get("/v2/enterprises/ent-ba/modules/basicinfo", params, true));
    }
}
