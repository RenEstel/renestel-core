Корневой проект для хранения родительского pom и общих утилит

## Что внутри?

| модуль | описание |
|--------|----------|
|        |          |

## Как подключить?

#### Maven:

Унаследовать от корневого pom:

```.xml
<parent>
    <groupId>io.github.renestel</groupId>
    <artifactId>renestel-core</artifactId>
    <version>LATEST_RELEASE</version>
</parent>
```

Подключить желаемый модуль, если он необходим:

```xml
<dependency>
    <groupId>io.github.renestel</groupId>
    <artifactId>MODULE_NAME</artifactId>
</dependency>
```
