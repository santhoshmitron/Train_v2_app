package com.jtrack.cluster;
import akka.cluster.sharding.ShardRegion;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jtrack.pojo.Gate;

public class J_Extractor implements ShardRegion.MessageExtractor {

    private static final Logger logger = LoggerFactory.getLogger(J_Extractor.class);
    Config jtrackConfig;
    int shardId;

    public J_Extractor(){
        jtrackConfig = ConfigFactory.load();
        shardId = jtrackConfig.getInt("jtrack.kafka.consumer.cluster.shardId");
    }

    @Override
    public String entityId(Object message) {

        if(message instanceof Gate){
            String msg = ((Gate) message).getGateName();
            String hash = msg;
            int id = hash.hashCode();
            return Integer.toString(id);
        }
        return null;
    }

    @Override
    public Object entityMessage(Object message) {
        if(message instanceof Gate){
            logger.info("J_ClusterInit: entityMessage: " +((Gate) message).getGateName());
            return message;
        }
        return null;
    }

    @Override
    public String shardId(Object message) {

        if( message instanceof  Gate){
            // Adding UniquenessKey and TstampTrans.
            String key = ((Gate) message).getGateName();
            String hash = key;
            int id = hash.hashCode();
            String val = String.valueOf(id%shardId);
            return val;
        }
        return null;
    }
}
