package helper;


import software.amazon.awssdk.auth.credentials.*;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import java.util.HashMap;
import java.util.Map;
public class SSMExample {

    public static String getValueOfSsmParam(AwsCredentialsProvider credentialsProvider, String key){
        GetParameterRequest parameterRequest = GetParameterRequest.builder()
                .name(key)
                .build();
        SsmClient ssmClient = SsmClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(credentialsProvider)
                .build();
        GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
        String parameterValue = parameterResponse.parameter().value();
//        System.out.println("Parameter value: " + parameterValue);
        // Close the SSM client
        ssmClient.close();
        return  parameterValue;
    }

    public static Map<String, String> authenticateUser(){
        String profileName = "default";
        AwsCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder()
                .profileName(profileName)
                .build();
        String UserPoolId = getValueOfSsmParam(credentialsProvider,"CognitoUserPoolId");
        String clientId = getValueOfSsmParam(credentialsProvider,"CognitoClientId");
        String username = getValueOfSsmParam(credentialsProvider,"V3TestUser");
        String password = getValueOfSsmParam(credentialsProvider,"V3TestPassword");

        // Create an instance of the Cognito Identity Provider client
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.US_EAST_2) // Set the appropriate region
                .credentialsProvider(credentialsProvider)
                .build();

        // Set up the authentication request
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", username);
        authParams.put("PASSWORD", "@Testing123");

        // Set up the authentication request
        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(
                        authParams
                )
                .build();

        // Initiate the authentication process
        InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

        // Retrieve the authentication result
        AuthenticationResultType authResult = authResponse.authenticationResult();

        // Print the authentication result
//        System.out.println("Access Token: " + authResult.accessToken());
//        System.out.println("ID Token: " + authResult.idToken());
//        System.out.println("Refresh Token: " + authResult.refreshToken());

        // Close the Cognito client
        cognitoClient.close();

        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("accesstoken", authResult.accessToken());
        headersMap.put("Authorization", "Bearer " + authResult.idToken());

      return headersMap;
    }

    public static void main(String[] args) {
        authenticateUser();
    }
}
