# OpenX

O **OpenX** é um aplicativo Android criado para ajudar na tradução de jogos feitos em **Ren’Py** e de textos presentes em **APKs Android**, direto pelo celular. Ele reúne os arquivos em projetos para facilitar a tradução, a revisão e a exportação.

## Para que serve

- **Jogos Ren’Py:** importar scripts `.rpy`, editar diálogos e opções de menu e exportar arquivos de tradução.
- **APKs Android:** inspecionar o pacote, identificar arquivos de texto acessíveis e importar esses textos para tradução.
- **Edição e revisão:** organizar traduções por projeto, manter um glossário e verificar variáveis e marcações nos textos Ren’Py.
- **Assistência à tradução:** usar os mecanismos disponíveis no app, incluindo opções locais e provedores de IA configurados pelo usuário.
- **Exportação:** salvar os textos traduzidos em arquivos ou em um ZIP para integrar ao jogo ou aplicativo.

### Suporte a arquivos compilados

O suporte a APKs depende dos arquivos encontrados dentro do pacote. A versão atual trabalha com textos acessíveis e exporta os arquivos traduzidos; reconstruir e assinar um APK traduzido exige ferramentas externas. Recursos Android compilados e arquivos Ren’Py `.rpa` e `.rpyc` também precisam de extração ou descompilação externa antes da edição.

## Baixar e instalar

[Baixar OpenX.apk](https://github.com/BAZZOOKA999/openx/releases/download/apk-latest/OpenX.apk)

Requer Android 7.0 ou superior. O APK é uma versão de teste assinada com chave de depuração. No Android, permita a instalação de aplicativos pelo navegador ao abrir o arquivo.

## Compilação

O GitHub Actions compila o aplicativo e executa os testes a cada atualização da branch `main`. O APK fica disponível em Releases e nos artefatos da execução concluída.

Para compilar localmente, use Android Studio ou Java 17, Android SDK e Gradle 9.3.1. Gere `debug.keystore` na raiz usando a chave de depuração Android (alias `androiddebugkey`, senhas `android`) e execute `gradle assembleDebug testDebugUnitTest`.

As chaves dos provedores de IA devem ser configuradas no aplicativo. Nenhuma chave privada está incluída neste repositório. Recursos que usam serviços externos dependem da configuração e disponibilidade do provedor.
