# Meu IPTV — player IPTV para Android TV

App Android TV simples para assistir canais das listas públicas do
[iptv-org](https://github.com/iptv-org/iptv) e do [Free-TV](https://github.com/Free-TV/IPTV),
ou de qualquer lista M3U que você adicionar.

## Recursos
- Listas prontas: Brasil, Portugal, canais em português, Free-TV, Notícias, Esportes,
  Filmes, Séries, Infantil, Música, Documentários e a lista completa do iptv-org.
- Adicione suas próprias listas M3U (item **➕ Adicionar lista M3U**).
- Categorias automáticas (a partir do `group-title` da lista) e busca por nome.
- Favoritos (segure **OK** em um canal).
- Player ExoPlayer (Media3) com suporte a HLS, DASH e streams diretos; respeita
  `user-agent` e `referrer` definidos na lista.
- Cache das listas por 6 h (funciona mesmo se o GitHub estiver lento).
- Canais adultos (XXX) são filtrados.

## Controle remoto
| Onde | Botão | Ação |
|---|---|---|
| Menu | OK | Abrir lista / categoria / canal |
| Menu | Segurar OK no canal | Adicionar/remover favorito |
| Menu | Segurar OK numa lista pronta | Recarregar da internet |
| Menu | Segurar OK numa lista sua | Remover a lista |
| Player | ▲ / CH+ | Próximo canal |
| Player | ▼ / CH− | Canal anterior |
| Player | OK / ◀ / ▶ | Mostrar nome do canal |
| Player | Voltar | Voltar ao menu |

## Como gerar o APK

### Opção A — sem instalar nada (GitHub Actions)
1. Crie um repositório no GitHub e envie esta pasta (pode ser pelo site: *Add file → Upload files*,
   arrastando tudo, inclusive a pasta `.github`).
2. Abra a aba **Actions**, escolha **Gerar APK** e clique em **Run workflow**
   (ele também roda sozinho a cada envio).
3. Ao terminar (~5 min), abra a execução e baixe o artefato **MeuIPTV-apk**
   (um .zip com o `app-release.apk`).

### Opção B — Android Studio
1. Abra a pasta no Android Studio (Hedgehog ou mais novo) e espere o Gradle sincronizar.
2. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
3. O APK fica em `app/build/outputs/apk/`.

## Como instalar na TV
- **Pelo app Downloader** (Android TV / Fire TV): ative *Fontes desconhecidas* para o Downloader,
  coloque o APK em algum link direto (Google Drive, Dropbox etc.) e abra o link no Downloader.
- **Por pendrive**: copie o APK e abra com um gerenciador de arquivos na TV
  (ex.: *File Commander*, *X-plore*).
- **Por ADB** (TV e PC na mesma rede, *Depuração USB/ADB* ativada nas opções de desenvolvedor):
  `adb connect IP_DA_TV` e depois `adb install app-release.apk`.

O app aparece na tela inicial da TV com o banner "Meu IPTV".

## Observações
- Os canais vêm de listas públicas mantidas pela comunidade: muitos ficam fora do ar ou são
  bloqueados por região de tempos em tempos. Se um canal não abrir, use ▲/▼ para ir ao próximo.
- O iptv-org só indexa links de transmissões disponibilizadas publicamente; ele não hospeda vídeo.
- Para personalizar as listas prontas, edite `Sources.defaults` em
  `app/src/main/java/com/meuiptv/tv/Models.kt`.

## Estrutura
```
app/src/main/java/com/meuiptv/tv/
  MainActivity.kt        tela principal (listas, categorias, canais)
  PlayerActivity.kt      player em tela cheia
  M3uParser.kt           leitura das playlists M3U
  PlaylistRepository.kt  download + cache
  Storage.kt             favoritos e listas personalizadas
  Adapters.kt            listas com foco para controle remoto
  Models.kt              modelos e listas padrão
```
