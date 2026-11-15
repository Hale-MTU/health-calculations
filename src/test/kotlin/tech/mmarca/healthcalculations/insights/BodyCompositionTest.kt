package tech.mmarca.healthcalculations.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BodyCompositionTest {
    @Test
    fun `body mass index is weight over height squared`() {
        assertThat(bodyMassIndex(72.0, 180.0)).isWithin(1e-9).of(72.0 / (1.8 * 1.8))
    }

    @Test
    fun `body mass index needs a weight and a positive height`() {
        assertThat(bodyMassIndex(null, 180.0)).isNull()
        assertThat(bodyMassIndex(72.0, null)).isNull()
        assertThat(bodyMassIndex(72.0, 0.0)).isNull()
    }

    @Test
    fun `fat-free mass index leaves the fat out`() {
        assertThat(fatFreeMassIndex(80.0, 200.0, 25.0)).isWithin(1e-9).of(60.0 / 4.0)
        assertThat(fatFreeMassIndex(80.0, 200.0, 0.0)).isWithin(1e-9).of(20.0)
    }

    @Test
    fun `fat-free mass index is unknown without a believable input`() {
        assertThat(fatFreeMassIndex(0.0, 180.0, 20.0)).isNull()
        assertThat(fatFreeMassIndex(80.0, 0.0, 20.0)).isNull()
        assertThat(fatFreeMassIndex(80.0, 180.0, null)).isNull()
        assertThat(fatFreeMassIndex(80.0, 180.0, -1.0)).isNull()
        assertThat(fatFreeMassIndex(80.0, 180.0, 100.0)).isNull()
    }

    @Test
    fun `the adjusted index adds 6_3 per metre under 1_80`() {
        assertThat(adjustedFatFreeMassIndex(80.0, 180.0, 25.0)).isWithin(1e-9).of(fatFreeMassIndex(80.0, 180.0, 25.0)!!)
        assertThat(adjustedFatFreeMassIndex(80.0, 170.0, 25.0))
            .isWithin(1e-9).of(fatFreeMassIndex(80.0, 170.0, 25.0)!! + 0.63)
        assertThat(adjustedFatFreeMassIndex(80.0, 170.0, 100.0)).isNull()
    }
}
