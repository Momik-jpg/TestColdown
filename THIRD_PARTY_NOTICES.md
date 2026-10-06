# Open-Source-Nachweise

TestColdown ist unter MIT lizenziert; der vollständige Text steht in `LICENSE` und offline in der App. Die mitgelieferten Illustrationen und ihr Entstehungsnachweis stehen in `docs/graphics-provenance.md`.

Die Android-Ausgaben verwenden AndroidX, Jetpack Compose, Material Components, Kotlin und Kotlinx sowie deren indirekte Laufzeitabhängigkeiten. `generateDebugLegalNotices`, `generatePreviewLegalNotices` und `generateReleaseLegalNotices` ermitteln die tatsächlich aufgelösten Module und Versionen ihrer jeweiligen Runtime-Classpath. Maven-POM-Lizenzangaben werden einschließlich übergeordneter POMs übernommen. Fehlende oder nicht aufgelöste Angaben stoppen den Build zur Prüfung.

Die generierte Datei `legal/licenses.md` enthält die Modul-/Versionsliste, Lizenznamen und Quellen, den MIT-Projekttext, den vollständigen Apache-2.0-Text und originale LICENSE-, NOTICE- und COPYING-Dateien aus den JAR-/AAR-Artefakten einschließlich eingebetteter JARs. Sie wird in jede APK/AAB eingebunden und ist unter „Optionen → Hilfe & Version → Lizenzen & Bildnachweise“ ohne Internet lesbar und kopierbar. Ausgeschlossene doppelte META-INF-Dateien werden dadurch nicht als alleiniger Lizenznachweis verwendet.

Upstream-Quellen: https://developer.android.com/jetpack/androidx, https://github.com/material-components/material-components-android, https://github.com/JetBrains/kotlin, https://github.com/Kotlin/kotlinx.serialization und https://www.apache.org/licenses/LICENSE-2.0.

Ein automatischer Metadatenbericht ersetzt keine Prüfung neuer Abhängigkeiten und ihrer Verteilung. Vor einer Veröffentlichung mit zusätzlichen Bibliotheken müssen insbesondere abweichende Lizenztexte, NOTICE-Pflichten, Marken und eingebettete Fremdkomponenten geprüft und gegebenenfalls ergänzt werden.
