# OpenX

Aplicativo Android para projetos de tradução, incluindo arquivos Ren’Py e recursos Android.

## Baixar e instalar

[Baixar OpenX.apk](https://github.com/BAZZOOKA999/openx/releases/download/apk-latest/OpenX.apk)

Requer Android 7.0 ou superior. O APK é uma versão de teste assinada com chave de depuração. No Android, permita a instalação de aplicativos pelo navegador ao abrir o arquivo.

## Compilação

O GitHub Actions compila o aplicativo e executa os testes a cada atualização da branch `main`. O APK fica disponível em Releases e nos artefatos da execução concluída.

Para compilar localmente, use Android Studio ou Java 17, Android SDK e Gradle 9.3.1. Gere `debug.keystore` na raiz usando a chave de depuração Android (alias `androiddebugkey`, senhas `android`) e execute `gradle assembleDebug testDebugUnitTest`.

As chaves dos provedores de IA devem ser configuradas no aplicativo. Nenhuma chave privada está incluída neste repositório. Recursos que usam serviços externos dependem da configuração e disponibilidade do provedor.
