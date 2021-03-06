package com.streamxhub.streamx.flink.quickstart.connector

import com.streamxhub.streamx.flink.connector.jdbc.sink.JdbcSink
import com.streamxhub.streamx.flink.connector.kafka.source.KafkaSource
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import org.apache.flink.api.common.typeinfo.TypeInformation

object JdbcSinkApp extends FlinkStreaming {

  implicit val stringType: TypeInformation[String] = TypeInformation.of(classOf[String])

  override def handle(): Unit = {

    /**
     * 从kafka里读数据.这里的数据是数字或者字母,每次读取1条
     */
    val source = KafkaSource().getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })


    /**
     * 假设这里有一个orders表.有一个字段,id的类型是int
     * 在数据插入的时候制造异常:
     * 1)正确情况: 当从kafka中读取的内容全部是数字时会插入成功,kafka的消费的offset也会更新.
     * 如: 当前kafka size为20,手动输入10个数字,则size为30,然后会将这10个数字写入到Mysql,kafka的offset也会更新
     * 2)异常情况: 当从kafka中读取的内容非数字会导致插入失败,kafka的消费的offset会回滚
     * 如: 当前的kafka size为30,offset是30, 手动输入1个字母,此时size为31,写入mysql会报错,kafka的offset依旧是30,不会发生更新.
     */
    JdbcSink(parallelism = 5).sink[String](source)(x => {
      s"insert into orders(id,timestamp) values('$x',${System.currentTimeMillis()})"
    }).uid("mysqlSink").name("mysqlSink")

  }

}
