package com.example.fn;

import com.alibaba.fastjson.JSONObject;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeManagementClient;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.core.responses.GetInstancePoolResponse;
import com.oracle.bmc.core.responses.UpdateInstancePoolResponse;

import java.io.IOException;

public class HelloFunction {

    public String handleRequest(String input) throws IOException {

        System.out.println("Inside Autoscaling Function");

        String name = (input == null || input.isEmpty()) ? "Autoscaling" : "Autoscaling";

        int size = 0;

        try {
            // input from Alarm then Notification it will be Json Format
            JSONObject inputJson = JSONObject.parseObject(input);
            size = Integer.valueOf((String) inputJson.get("body"));
            String type = (String) inputJson.get("type");
            // if type is FIRING_TO_OK then return
            if("FIRING_TO_OK".equals(type)){
                System.out.println("not set size to " + size);
                System.out.println("type is " + type);
                return "Hello, " + name + "!";
            } else {
                System.out.println("set size to " + size);
                System.out.println("type is " + type);
            }
        } catch (Exception e) {
            System.out.println("Exception:" + e);
            try {
                // input from Notifications input should be Integer
                size = Integer.valueOf(input);
                System.out.println("set size to " + size);
                System.out.println("type is NOTIFICATION");
            }catch (NumberFormatException nfe){
                System.out.println("NumberFormatException:" + nfe);
                // if input is not an available Number then return
                return "Hello, " + name + "!";
            }
        }

        // use AuthenticationDetailsProvider
        String configurationFilePath = "/function/app/.oci/config";
        String profile = "DEFAULT";

        // Configuring the AuthenticationDetailsProvider. It's assuming there is a default OCI config file
        // "~/.oci/config", and a profile in that config with the name "DEFAULT". Make changes to the following
        // line if needed and use ConfigFileReader.parse(CONFIG_LOCATION, profile);
        final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configurationFilePath, profile);

        // use AuthenticationDetailsProvider for local deployment
//        final AuthenticationDetailsProvider provider =
//                new ConfigFileAuthenticationDetailsProvider(configFile);

        // use ResourcePrincipalAuthenticationDetailsProvider while deploying to OCI Function
        final ResourcePrincipalAuthenticationDetailsProvider provider
                = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

        ComputeManagementClient client = new ComputeManagementClient(provider);
        // must set region while using ResourcePrincipalAuthenticationDetailsProvider
        client.setRegion(Region.fromRegionId("ap-seoul-1"));

        // get current size
        GetInstancePoolRequest.Builder getBuilder = GetInstancePoolRequest.builder().instancePoolId("ocid1.instancepool.oc1.ap-seoul-1.aaaaaaaae2xtthkkmo57yx6lsqg4ehmcfufnqvimtxlomybazi7llqbiouea");
        GetInstancePoolRequest getInstancePoolRequest = getBuilder.build();
        GetInstancePoolResponse getInstancePoolResponse = client.getInstancePool(getInstancePoolRequest);
        InstancePool autoscalingInstancePool = getInstancePoolResponse.getInstancePool();

        // update to size
        if (size != autoscalingInstancePool.getSize()) {
            UpdateInstancePoolRequest.Builder updateBuilder = UpdateInstancePoolRequest.builder().instancePoolId("ocid1.instancepool.oc1.ap-seoul-1.aaaaaaaae2xtthkkmo57yx6lsqg4ehmcfufnqvimtxlomybazi7llqbiouea");
            UpdateInstancePoolDetails updateInstancePoolDetails = UpdateInstancePoolDetails.builder().size(size).build();
            UpdateInstancePoolRequest updateInstancePoolRequest = updateBuilder.body$(updateInstancePoolDetails).build();
            UpdateInstancePoolResponse updateInstancePoolResponse = client.updateInstancePool(updateInstancePoolRequest);
            System.out.print(autoscalingInstancePool.getDisplayName() + "'s size is scaled from ");
            System.out.print(autoscalingInstancePool.getSize() + " to ");
            System.out.print(updateInstancePoolResponse.getInstancePool().getSize());
        } else {
            System.out.print(autoscalingInstancePool.getDisplayName() + "'s size is " + size);
        }

        client.close();

        return "Hello, " + name + "!";
    }

}
