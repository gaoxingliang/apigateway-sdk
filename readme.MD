# 关于
四川征信api网关的示例sdk。 查看测试代码：[Test.java](./src/test/java/Test.java)。
文档单独提供。
## how to use
- step1:import this dependency
Check the latest version at: [maven central](https://mvnrepository.com/artifact/io.gitee.codeed/apigateway-sdk)
gradle:
```shell
implementation group: 'io.gitee.codeed', name: 'apigateway-sdk', version: 'XXXX'
```

mvn:
```xml
<dependency>
    <groupId>io.gitee.codeed</groupId>
    <artifactId>apigateway-sdk</artifactId>
    <version>XXXX</version>
</dependency>
```
- step 2 refer to test case:
```java
        ApiConfig config = new ApiConfig();
        config.setUsername("usernameXXX");
        config.setPassword("passwordXXX");
        config.setUrl("http://devicbc.sichuancredit.cn:88");
        config.setPrivateKey("yourPrivateKey");
        config.setPublicKey("publicKeyWeProvided");

        ApiClient apiClient = new ApiClient(config);
        Map<String, Object> params = new HashMap<>();
        params.put("enterprise", "四川征信有限公司");
        System.out.println(apiClient.get("/v2/enterprises/ent-ba/modules/basicinfo", params, true));
```

# how to build
some envs are stored in local ~/.zshrc
```shell
export JRELEASER_GPG_PASSPHRASE=
export JRELEASER_GITHUB_TOKEN=
export JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_USERNAME=
export JRELEASER_DEPLOY_MAVEN_MAVENCENTRAL_SONATYPE_PASSWORD=
```

gpg files are stored in local vm. 
```shell
        publicKey = '/Users/edward/.jreleaser/public.pgp'
        secretKey = '/Users/edward/.jreleaser/private.pgp'
```

Now it's manually uploaded by guide [here](https://central.sonatype.org/publish/publish-portal-upload/#switching-to-ossrh-during-portal-early-access).
So the commands are:<br>
```shell
./gradlew clean
./gradlew publish
./gradlew jreleaserFullRelease
# upload this bundle zip to: https://central.sonatype.com/publishing
ls ./build/jreleaser/deploy/mavenCentral/sonatype
```
<br>


It should use gradle command by guide [here](https://jreleaser.org/guide/latest/examples/maven/maven-central.html#_gradle)