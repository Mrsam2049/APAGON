# ApaGón: Salva tu Ciudad — Contexto del proyecto

## Stack
- Kotlin + Android Studio
- Arquitectura MVVM
- Jetpack: ViewModel, StateFlow, LiveData, ViewBinding
- Room (SQLite local)
- Retrofit 2 + Gson
- Coroutines
- Material Design 3

## APIs
- SiMEM GET: https://www.simem.co/backend-files/api/PublicData?datasetid=E17D25
- SINERGOX POST: https://servapibi.xm.com.co/daily
  Body: { "MetricId": "VoluUtilDiarEner", "StartDate": "...", "EndDate": "...", "Entity": "Sistema" }

## Mecánica de juego
- 2 modos: Campaña (5 noches) y Supervivencia (infinito)
- 3 municipios: Apartadó, Quibdó, Riohacha
- Cada municipio tiene 4 distritos con personalidad:
  - hospital: no apagar 2 noches seguidas o game over
  - industrial: da +$6M encendido, cuesta apagarlo
  - residencial: descontento acumulado
  - rural: si se abandona 3 veces, se pierde
- Energía disponible < demanda total → siempre hay sobrecarga
- Cartas (mano de 4, 2 cuadrillas/jugadas por noche)
- Sinergia: Campaña Ciudadana antes de apagar → -60% penalización social
- 4 indicadores: cobertura (calc. desde distritos), presupuesto, bienestar social, índice ambiental

## GameEngine (clase Kotlin pura, sin UI)
Calcula:
- coverage = poweredDemand / totalDemand * 100
- socialPenalty por distrito apagado según trait
- budgetDelta del distrito industrial
- intensidad El Niño en supervivencia: 1 + (day-1) * 0.18
- condición de derrota: cualquier indicador <= 0, o hospital apagado 2 noches seguidas

## Pantallas (Activities/Fragments)
- SplashActivity → fetch APIs → Room
- MenuActivity → SelectModoFragment
- SelectMunicipioFragment → RecyclerView con 3 ciudades
- GameActivity (host) → EscenarioNocheFragment → GestionRedFragment → ReporteNocturnoFragment
- GameOverFragment
- ResumenFinalActivity
- HistorialActivity

## Dependencias build.gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'androidx.room:room-runtime:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
implementation 'com.google.android.material:material:1.11.0'