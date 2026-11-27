In-game screenshots of the skin packs, for the store's Skins page (/loja/skins/).

Put each pack's pictures in its own folder, named by the pack's id in src/config/store.ts:
silvestre/, glacial/, draconico/, abissal/, celestial/. Any png, jpg or webp works; they show
up on the page by themselves, in file-name order (01.webp, 02.webp...), and are resized for
the web when the site is built. A pack without pictures shows a placeholder.
