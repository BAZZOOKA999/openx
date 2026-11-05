# OpenX

Aplicativo Android para organizar traduções de jogos Ren’Py e textos acessíveis em APKs, com revisão, glossário, assistência por IA e exportação. Inclui uma biblioteca de jogos e um player experimental para scripts Ren’Py simples.

## Baixar

[Baixar OpenX.apk](https://github.com/BAZZOOKA999/openx/releases/download/apk-latest/OpenX.apk)

Requer Android 7.0 ou superior. O APK é uma versão de teste assinada com chave de depuração. Permita a instalação pelo navegador ao abrir o arquivo.

## Recursos

- Importação e edição de textos Ren’Py, organização por projeto, glossário e revisão de variáveis e marcações.
- Inspeção de textos acessíveis em APKs e exportação de traduções em arquivos ou ZIP.
- Assistência de tradução por provedores configurados pelo usuário e recursos locais disponíveis.
- Biblioteca de jogos importados por ZIP, detecção estimada da versão do Ren’Py e diagnóstico dos arquivos encontrados.
- Player experimental de scripts simples, com diálogos, escolhas, histórico, avanço automático, alternância de tradução e salvamento rápido.
- Demonstração incluída: O Mistério de Verdeluz.
- Gerenciamento de saves, com backup e restauração.
- Interface de gerenciamento de modelos GGUF.

## Limitações desta versão

O player usa um interpretador simplificado em Kotlin. Não inclui o runtime completo Ren’Py/Python/SDL e não garante compatibilidade com jogos Ren’Py 6, 7 ou 8. Comandos Python não são executados; comandos de áudio são apenas registrados no log. Quando não encontra um script principal simples, o player pode abrir uma demonstração. A detecção de versão não comprova que o jogo será executado.

Arquivos compilados `.rpa` e `.rpyc` exigem ferramentas externas de extração/descompilação. O app exporta textos traduzidos; reconstruir e assinar APKs traduzidos também exige ferramentas externas.

A execução de modelos GGUF depende da integração e compilação do motor nativo llama.cpp, que ainda não está incluído neste APK. Os serviços externos de IA dependem das credenciais e disponibilidade de cada provedor.

## Atualização e dados

Faça backup dos projetos e saves antes de atualizar. O projeto exportado usa recriação destrutiva do banco quando a versão do esquema muda. A chave de depuração é gerada em cada compilação, portanto o Android pode exigir a desinstalação da versão anterior, com perda dos dados locais.

## Compilação

O GitHub Actions compila o APK, executa os testes unitários e publica o arquivo em Releases. A aprovação desses testes não substitui testes de instalação e uso em aparelho Android.

Para compilar localmente, use Java 17, Android SDK e Gradle 9.3.1. Gere `debug.keystore` na raiz com alias `androiddebugkey` e senhas `android`, e execute `gradle assembleDebug testDebugUnitTest`.

Configure credenciais de IA no aplicativo ou no ambiente local, conforme o provedor. Não publique arquivos `.env` nem chaves privadas.
