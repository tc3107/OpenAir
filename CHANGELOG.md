# Changelog

All notable changes to this project will be documented in this file.
The format is based on Keep a Changelog and the project adheres to Semantic Versioning.

## [1.2.3] - 2026-02-07
### Added
- Privacy section in Config with expandable details about app data handling.
- Config toggle to allow or disallow keeping the playback service active in the background.

### Changed
- Help content in Config is now presented in an expandable "Read more" panel.
- Playback service startup and task-removal behavior now follows the background media service setting.
- Stopping playback now clears queued media items and stops the playback service.

## [1.2.2] - 2026-02-05
### Changed
- Disabled dependency metadata in APKs and app bundles.

## [1.2.1] - 2026-02-04
### Fixed
- Browse filter fields now scroll when the keyboard is open, preventing other filters from being squished.
- Fixed several smaller issues.

## [1.2.0] - 2026-02-04
### Added
- Fastlane metadata translations (short and full descriptions) for ro, es, pt-BR, de, fr, it, tr, pl, uk, and ru.

### Changed
- Database summary now counts unique tags and languages based on downloaded stations.
- System backups now include only playlist data (including favorites and recents) and exclude cached data and the station database.
- Config now hides non-database UI (including bottom bar) while a database rebuild is in progress and notes that the rebuild is only required once to get started.

### Fixed
- Backup/data extraction rules no longer include invalid exclude entries outside the playlist backup scope.
- Tapping the media playback notification now opens the app.
- Playback controls now reflect the current playing state after relaunching the app.

## [1.1.3] - 2026-02-03
### Changed
- Updated application package/namespace to `com.tudorc.openair`.
- Config database rebuild now shows determinate download progress up to 50,000 stations before switching to indeterminate.
- Added a dismissible playback error banner with clearer network/stream error messages.
- Split Config into Database/Playlists/Help/Support development sections with Database-only fallback when empty/error.
- Added app version info to Help and a Ko-fi support button in Support development.

## [1.1.2] - 2026-02-02
### Added
- Playlist station overflow menu with Playlists/Reorder/Remove actions.
- Playlist station reordering and removal support for favorites, recents, and user playlists.

### Changed
- Playlist station lists now keep their stored order to reflect reordering.

### Removed
- Custom URL stations feature and related UI (Custom playlist, add/edit flow, and playback handling).

## [1.1.0] - 2026-02-02
### Added
- Full database rebuild pipeline that downloads stations plus country/language/tag metadata, with progress and summary.
- Config screen with “Rebuild database from web” action and status/disclaimer messaging.
- Config help section, GitHub link, and refreshed layout cards for clarity.
- In-memory station filtering with a determinate progress indicator for Browse searches.
- Sort-by options for votes, clicks, and distance (with location prompt).
- Playlist search now includes user playlists and stations inside them.
- Playlist export/import with merge/replace modes, checksum validation, and custom URL station support.

### Changed
- Browse redesign into a single search/filter screen (country, language, tag, min votes) with live updates.
- Search bar filters station names only.
- “Near Me” is now handled via the distance sort in Browse.
- App restores the last used screen except Config (defaults to Browse); re-tapping tabs/back returns to main list or filter view.
- User-Agent string now includes app version and GitHub URL.
- Playlist search order now shows playlists before stations with a divider.
- Filter dropdowns include option dividers, clear invalid entries on blur, and min-votes accepts digits only.
- Config import/export shows errors via toast notifications only (no success messaging).
- Media notification metadata now uses “OpenAir Live” with station name subtitle and artwork when available.

### Fixed
- Play now works after app restart when a station is already in Now Playing.
- Config screen no longer flashes on startup before database status loads.
- Search back behavior now closes the keyboard before navigating back.
- Saveable state crash when leaving browse filters.

## [1.0.0] - 2026-02-02
### Added
- Browse stations by country, tag, and language with search filters.
- Station lists with pagination and sorting by votes/reliability.
- "Near Me" discovery using location permissions and geo-filtered stations.
- Playback with Media3 foreground service and retry across multiple stream URLs.
- Playlists: favorites, recents, custom stations, and user playlists (create/rename/delete/reorder).
- Persistence with DataStore and local caching for API responses.
- Station artwork loading with Coil and haptic feedback on interactions.
