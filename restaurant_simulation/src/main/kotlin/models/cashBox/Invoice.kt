package models.cashBox

import models.customers.CostumerGroup
import models.rating.Rating
import models.timers.Time
import utilities.Utilities

class Invoice(time: Time, costumerGroup: CostumerGroup?, waiterRating: Rating?) {
    val id: Long
    val time: Time
    val costumerGroup: CostumerGroup?
    val waiterRating: Rating?
    var paymentMethod: PaymentMethod
    val paymentType: PaymentType

    init {
        id = idCounter++
        this.time = time
        this.costumerGroup = costumerGroup
        this.waiterRating = waiterRating
        val randomMethod = Utilities.randomNumber(0, 2)
        val randomType = Utilities.randomNumber(0, 1)
        paymentMethod = if (randomMethod == 0) PaymentMethod.AMERICANO else if (randomMethod == 1) PaymentMethod.DIVIDIDO else PaymentMethod.UNICO
        paymentMethod = if (randomMethod == 0) PaymentMethod.AMERICANO else if (randomMethod == 1) PaymentMethod.DIVIDIDO else PaymentMethod.UNICO
        paymentType = if (randomType == 1) PaymentType.CASH else PaymentType.CREDIT_CARD
    }

    companion object {
        private var idCounter: Long = 1
    }
}