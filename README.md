# Elasticsearch使用示例

如果需要使用elasticsearch首先需要在 common模块的pom.xml文件中加入如下maven坐标

```xml
<dependency>
 	<groupId>com.g7.framework</groupId>
 	<artifactId>elasticsearch-reactive-spring-boot-autoconfigure</artifactId>
</dependency>
```

## Apollo配置

```properties
spring.elasticsearch.uris = https://paas-test-node1.es.test.chinawayltd.com:7200,https://paas-test-node2.es.test.chinawayltd.com:7200
spring.elasticsearch.username = xxxxxx
spring.elasticsearch.password = xxxxxx
```

## Repository操作方式

定义实体类

```java
@Data
@Document(indexName = "ntocc_oplog_index",shards = 24)
public class OpLogIndex extends AbstractIndex<String> implements Serializable {
 
    private static final long serialVersionUID = 958777181788694154L;
 
    @Field(name = "orgroot", type = FieldType.Keyword, docValues = false)
    private String orgroot;
    @Field(name = "orgcode", type = FieldType.Keyword, docValues = false)
    private String orgcode;
    @Field(name = "log_id", type = FieldType.Keyword)
    private String logId;
    @Field(name = "one_level")
    private Integer oneLevel;
    @Field(name = "two_level")
    private Integer twoLevel;
    @Field(name = "three_level")
    private Integer threeLevel;
    @Field(name = "operator_id", type = FieldType.Keyword, docValues = false)
    private String operatorId;
    @Field(name = "operator_type")
    private Integer operatorType;
    @Field(name = "operator_user_name")
    private String operatorUserName;
    @Field(name = "operator_real_name")
    private String operatorRealName;
    @Field(name = "operator_org_code", type = FieldType.Keyword, docValues = false)
    private String operatorOrgCode;
    @Field(name = "operator_org_name")
    private String operatorOrgName;
    @Field(name = "desc", type = FieldType.Keyword, docValues = false, index = false)
    private String desc;
    @Field(name = "waybill_id", type = FieldType.Keyword)
    private String waybillId;
    @Field(name = "waybill_no", type = FieldType.Keyword)
    private String waybillNo;
}
```

定义操作接口

```kotlin
interface OpLogIndexRepository : ReactiveElasticsearchRepository<OpLogIndex, String> {
 
    fun findAllByOperatorId(operatorId: String): Flux<OpLogIndex>
}
```

## Template操作方式

```java
@Autowired
private ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;
```

## 官方参考文档

https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/#preface