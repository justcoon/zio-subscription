Global / resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
Scope.Global / scalaVersion := "2.13.10"

lazy val Versions = new {
  val kindProjector = "0.13.2"
  val http4s = "0.23.14"
  val zio = "2.0.13"
  val zioInteropCats = "3.3.0"
  val zioLogging = "2.1.12"
  val zioMetrics = "2.0.8"
  val zioKafka = "2.2"
  val zioConfig = "4.0.0-RC14"
  val zioPrelude = "1.0.0-RC18"
  val jackson = "2.14.1"
  val circe = "0.14.3"
  val randomDataGenerator = "2.9"
  val logback = "1.4.7"
  val grpc = "1.54.1"
  val chimney = "0.6.2"
  val scalaJwt = "9.1.2"
  val quill = "4.6.0"
  val debezium = "2.2.0.Final"
  val flyway = "9.17.0"
}

lazy val library =
  new {
    // Scala libraries
    val zio = "dev.zio" %% "zio"                                                      % Versions.zio
    val zioStreams = "dev.zio" %% "zio-streams"                                       % Versions.zio
    val zioInteropCats = "dev.zio" %% "zio-interop-cats"                              % Versions.zioInteropCats
    val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j2"                           % Versions.zioLogging
    val zioLoggingSlf4jBridge = "dev.zio" %% "zio-logging-slf4j2-bridge"              % Versions.zioLogging
    val zioMetrics = "dev.zio" %% "zio-metrics-connectors"                            % Versions.zioMetrics
    val zioKafka = "dev.zio" %% "zio-kafka"                                           % Versions.zioKafka
    val jacksonModuleScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % Versions.jackson
    val http4sCore = "org.http4s" %% "http4s-core"                                    % Versions.http4s
    val http4sDsl = "org.http4s" %% "http4s-dsl"                                      % Versions.http4s
    val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server"                     % Versions.http4s
    val http4sCirce = "org.http4s" %% "http4s-circe"                                  % Versions.http4s
    val circeGeneric = "io.circe" %% "circe-generic"                                  % Versions.circe
    val circeGenericExtras = "io.circe" %% "circe-generic-extras"                     % Versions.circe

    val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % Versions.scalaJwt

    val chimney = "io.scalaland" %% "chimney" % Versions.chimney

    val zioConfigRefined = "dev.zio" %% "zio-config-refined"   % Versions.zioConfig
    val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig
    val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig

    val zioPrelude = "dev.zio" %% "zio-prelude" % Versions.zioPrelude

    val quillPostgres = "io.getquill" %% "quill-jasync-zio-postgres" % Versions.quill

    val grpcServices = "io.grpc"     % "grpc-services"     % Versions.grpc
    val grpcNetty = "io.grpc"        % "grpc-netty"        % Versions.grpc
    val grpcNettyShadded = "io.grpc" % "grpc-netty-shaded" % Versions.grpc

    val scalapbRuntime =
      "com.thesamet.scalapb" %% "scalapb-runtime"                             % scalapb.compiler.Version.scalapbVersion % "protobuf"
    val scalapbRuntimeGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

    val scalapbValidate =
      "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf"

    val zioTest = "dev.zio" %% "zio-test"                                      % Versions.zio                 % "test"
    val zioTestSbt = "dev.zio" %% "zio-test-sbt"                               % Versions.zio                 % "test"
    val randomDataGenerator = "com.danielasfregola" %% "random-data-generator" % Versions.randomDataGenerator % "test"

    // Java libraries
    val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    val debeziumApi = "io.debezium"               % "debezium-api"                % Versions.debezium
    val debeziumEmbedded = "io.debezium"          % "debezium-embedded"           % Versions.debezium
    val debeziumConnectorPostgres = "io.debezium" % "debezium-connector-postgres" % Versions.debezium

    val flywayCore = "org.flywaydb" % "flyway-core" % Versions.flyway
  }

lazy val `zio-subscription` =
  project
    .in(file("."))
    .enablePlugins(GitVersioning)
    .aggregate(`core`, `subscription-api`, `subscription-svc`)
    .settings(settings)
    .settings(
      Compile / unmanagedSourceDirectories := Seq.empty,
      Test / unmanagedSourceDirectories    := Seq.empty,
      publishArtifact                      := false
    )

lazy val `core` =
  (project in file("modules/core"))
    .settings(settings)
    .settings(
      addCompilerPlugin("org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full),
      Compile / PB.targets := Seq(
        scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
        scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
      ),
      PB.protocVersion := "3.17.3" // mac m1 issue
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "openapi")
    .settings(
      libraryDependencies ++= Seq(
        library.zio,
        library.zioStreams,
        library.zioInteropCats,
        library.zioLoggingSlf4j,
        library.zioMetrics,
        library.http4sCore,
        library.http4sDsl,
        library.circeGeneric,
        library.circeGenericExtras,
        library.jwtCirce,
        library.zioConfigRefined,
        library.zioConfigMagnolia,
        library.zioConfigTypesafe,
        library.scalapbRuntime,
        library.scalapbRuntimeGrpc,
        library.debeziumApi,
        library.debeziumEmbedded,
        library.debeziumConnectorPostgres,
        library.logback,
        library.zioTest,
        library.zioTestSbt
      ),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )

lazy val `subscription-api` =
  (project in file("modules/subscription-api"))
    .settings(settings)
    .settings(
      addCompilerPlugin("org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full),
      Compile / PB.targets := Seq(
        scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
        scalapb.validate.gen() -> (Compile / sourceManaged).value,
        scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
      ),
      PB.protocVersion := "3.17.3" // mac m1 issue
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "openapi")
    .settings(
      libraryDependencies ++= Seq(
        // Scala libraries
        library.zio,
        library.zioStreams,
        library.zioInteropCats,
        library.zioLoggingSlf4j,
        library.circeGeneric,
        library.circeGenericExtras,
        library.scalapbRuntime,
        library.scalapbRuntimeGrpc,
        library.scalapbValidate
      )
    )

lazy val `subscription-svc` =
  (project in file("modules/subscription-svc"))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .settings(settings ++ dockerSettings)
    .settings(
      addCompilerPlugin("org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full)
    )
    .settings(
      libraryDependencies ++= Seq(
        // Scala libraries
        library.zio,
        library.zioStreams,
        library.zioInteropCats,
        library.zioLoggingSlf4j,
        library.zioPrelude,
        library.jacksonModuleScala,
        library.http4sCore,
        library.http4sDsl,
        library.http4sBlazeServer,
        library.http4sCirce,
        library.circeGeneric,
        library.circeGenericExtras,
        library.chimney,
        library.grpcServices,
        library.grpcNetty,
        library.grpcNettyShadded,
        library.scalapbRuntime,
        library.scalapbRuntimeGrpc,
        library.zioKafka,
        library.quillPostgres,
        library.flywayCore,
        library.randomDataGenerator,
        library.logback,
        library.zioTest,
        library.zioTestSbt
      ),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )
    .aggregate(`subscription-api`)
    .dependsOn(`subscription-api`, `core`)

lazy val settings = commonSettings ++ gitSettings

lazy val commonSettings =
  Seq(
    organization             := "c",
    scalafmtOnCompile        := true,
    Test / fork              := true,
    Test / parallelExecution := true,
    licenses += ("Apache 2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    Compile / packageBin / mappings += (ThisBuild / baseDirectory).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explaintypes",
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
      "-language:experimental.macros", // Allow macro definition (besides implementation and application)
      "-language:higherKinds", // Allow higher-kinded types
      "-language:implicitConversions", // Allow definition of implicit functions called views
      "-unchecked",
      "-Xcheckinit",
      "-Xlint:adapted-args",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:inaccessible",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      //"-Xlint:unsound-match",
      //"-Yno-adapted-args",
      //"-Ypartial-unification",
      "-Ywarn-extra-implicit",
      //"-Ywarn-inaccessible",
      //"-Ywarn-nullary-override",
      //"-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    )
    //    scalacOptions ++= Seq(
    //      "-unchecked",
    //      "-deprecation",
    //      "-language:_",
    //      "-target:jvm-1.8",
    //      "-encoding",
    //      "UTF-8"
    //    ),
    //    javacOptions ++= Seq(
    //      "-source",
    //      "1.8",
    //      "-target",
    //      "1.8"
    //    )
  )

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val dockerSettings =
  Seq(
    Docker / maintainer := "justcoon",
//   Docker / version := "latest",
    dockerUpdateLatest := true,
    dockerExposedPorts := Vector(8000, 8010, 9080),
    dockerBaseImage    := "openjdk:11-jre"
  )
