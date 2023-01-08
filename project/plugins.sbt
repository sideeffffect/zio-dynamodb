addSbtPlugin("org.scalameta"      % "sbt-scalafmt"    % "2.5.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.0")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"   % "0.10.0")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"   % "1.6.1")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.2.16")
addSbtPlugin("ch.epfl.scala"      % "sbt-bloop"       % "1.4.6")
addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"      % "0.4.3")
addSbtPlugin("com.geirsson"       % "sbt-ci-release"  % "1.5.5")
addSbtPlugin("dev.zio"            % "zio-sbt-website" % "0.1.5+28-4b5b0374-SNAPSHOT")

resolvers += Resolver.sonatypeRepo("public")
