package com.moonkata.plantwater.recognition

// Gemini 인식 결과. wateringIntervalDays는 5번(일정등록) 슬라이더 기본값으로 씀
data class PlantIdentification(
    val commonName: String,
    val scientificName: String,
    val wateringIntervalDays: Int,
    val wateringAdvice: String,
    val lightAdvice: String
)

// 인식 실패 시 수동 선택 폴백용 카탈로그
data class PlantCatalogEntry(
    val name: String,
    val scientificName: String,
    val wateringIntervalDays: Int,
    val lightAdvice: String
)

object PlantCatalog {
    val entries = listOf(
        PlantCatalogEntry("몬스테라", "Monstera deliciosa", 7, "밝은 간접광"),
        PlantCatalogEntry("산세베리아", "Sansevieria trifasciata", 14, "약한 빛에도 잘 견딤"),
        PlantCatalogEntry("금전수", "Zamioculcas zamiifolia", 10, "밝은 간접광~약한 빛"),
        PlantCatalogEntry("스킨답서스", "Epipremnum aureum", 7, "밝은 간접광"),
        PlantCatalogEntry("선인장", "Cactaceae", 21, "직사광선 선호"),
        PlantCatalogEntry("기타 (직접 입력)", "미분류", 7, "정보 없음")
    )
}
