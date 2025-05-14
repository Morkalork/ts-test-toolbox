# Build and release

To build the project, run:

```bash
./gradlew build
```

To release, run:

```bash
./gradlew publish
```

To clear stuff up for when you've messed things up, run:

```bash
./gradlew clean
```

## Publishing token

In order to be able to release, you need a publishing token to enter as value for the environmentl variable `PUBLISH_TOKEN`. To get this,
talk to Magnus Ferm. If Magnus has died, this project dies with him.