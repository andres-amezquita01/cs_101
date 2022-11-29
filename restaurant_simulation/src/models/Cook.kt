package models

class Cook(specialy: SpecialtyType) {
    val id: Long
    private var avaliable: Boolean
    val specialy: SpecialtyType
    private var assignedSpeciality: Int
    var nextFreeTime: Time?
        private set
    val orderItemList: Array<OrderItem?>

    init {
        id = idCounter++
        this.specialy = specialy
        avaliable = true
        assignedSpeciality = 0
        nextFreeTime = Time(1, 1, 0, 0, 0)
        orderItemList = arrayOfNulls(SPECIAL_PLATE_LIMIT)
    }

    fun isAvaliable(specialtyType: SpecialtyType?, currentTime: Time?): Boolean {
        if (specialy == specialtyType && assignedSpeciality < SPECIAL_PLATE_LIMIT) {
            avaliable = true
        } else if (nextFreeTime!!.beforeThan(currentTime)) {
            resetOrderItemList()
            assignedSpeciality = 0
            avaliable = true
        }
        return avaliable
    }

    fun cookPlate(orderItem: OrderItem?, currentSimulation: Time?) {
        addOrderItem(orderItem)
        nextFreeTime = orderItem?.plate?.preparationTime
        nextFreeTime!!.addTime(currentSimulation)
        avaliable = false
        if (specialy == orderItem?.plateType) {
            assignedSpeciality++
        }
    }

    private fun addOrderItem(orderItem: OrderItem?) {
        if (orderItemList[0] != null) {
            orderItemList[0] = orderItem
        } else {
            orderItemList[1] = orderItem
        }
    }

    private fun resetOrderItemList() {
        for (i in orderItemList.indices) {
            orderItemList[i] = null
        }
    }

    companion object {
        private var idCounter: Long = 0
        private const val SPECIAL_PLATE_LIMIT = 2
    }
}