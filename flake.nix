{
  description = "Environnement de développement pour l'application Android Papra Share (Jetpack Compose).";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs?ref=nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
            config.android_sdk.accept_license = true;
          };

          # SDK Android minimal pour Compose : plateforme 36 + build-tools 35 (minimum AGP) et 36.
          androidSdk = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "36" ];
            buildToolsVersions = [ "36.0.0" "35.0.0" ];
            includeEmulator = false;
            includeNDK = false;
            includeCmake = false;
            includeSystemImages = false;
          };
        in
        {
          default = pkgs.mkShell {
            name = "papra-share-dev";

            packages = with pkgs; [
              jdk17
              gradle
              android-tools
              androidSdk.androidsdk
            ];

            # Gradle a besoin de localiser le SDK et un JDK 17.
            env.ANDROID_HOME = "${androidSdk.androidsdk}/libexec/android-sdk";
            env.ANDROID_SDK_ROOT = "${androidSdk.androidsdk}/libexec/android-sdk";
            env.JAVA_HOME = "${pkgs.jdk17}";

            shellHook = ''
              echo ""
              echo "════════════════════════════════════════════════"
              echo "  Papra Share — environnement Android prêt"
              echo "  JDK :    $JAVA_HOME"
              echo "  SDK :    $ANDROID_HOME"
              echo "  Gradle : $(gradle --version | grep Gradle)"
              echo "  CompileSdk 36 — build-tools 36.0.0"
              echo "════════════════════════════════════════════════"
              echo ""
              echo "  Build :  gradle assembleDebug"
              echo "  Test :   gradle test"
              echo ""
            '';
          };
        });
    };
}