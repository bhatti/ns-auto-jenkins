# NS Auto Jenkins Plugin

## [Docs](https://docs.nowsecure.com/auto/integration-services/jenkins-integration/)

## Building
```
mvn hpi:run
```

## Findbugs
```
mvn findbugs:gui
```

## Installing
```
mvn clean install
cp target/NSAutoJenkins.hpi ~/.jenkins/plugins/
```

Then redeploy Jenkins.