mvn deploy:deploy-file -Dfile=E:\jar\common-share.jar -DgroupId=com.dp.shop -DartifactId=shop-share -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DrepositoryId=snapshots -Durl=http://10.0.1.xx:8080/nexus/content/repositories/snapshots/

mvn deploy -DskipTests -P sonatype-oss-release