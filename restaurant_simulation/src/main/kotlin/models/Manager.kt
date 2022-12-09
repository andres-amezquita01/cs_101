package models

import models.cashBox.Invoice
import models.cashBox.PaymentType
import models.customers.CostumerGroup
import models.kitchen.Cook
import models.kitchen.Order
import models.kitchen.OrderItem
import models.kitchen.SpecialtyType
import models.kitchen.plates.DessertPlate
import models.kitchen.plates.EntreePlate
import models.kitchen.plates.MainCourse
import models.kitchen.plates.Plate
import models.rating.RatingType
import models.rating.Rating
import models.timers.Time
import persistence.FileOperations
import structures.CustomerQueu

class Manager {
    private val dessertPlateList = mutableListOf<DessertPlate>()
    private val mainCourseList= mutableListOf<MainCourse>()
    private val entreePlateList= mutableListOf<EntreePlate>()
    private val timeArrivalsClientList = FileOperations.readFile()

    var groupQueue: CustomerQueu<CostumerGroup?> = CustomerQueu(null)
    var orderQueue: CustomerQueu<Order?> = CustomerQueu(null)
    var paymentQueue: CustomerQueu<CostumerGroup> = CustomerQueu(null)
    var paymentPriorityQueue: CustomerQueu<CostumerGroup> = CustomerQueu(null)
    val invoiceList = mutableListOf<Invoice>()
    val waiterList: MutableList<Waiter> = mutableListOf(Waiter(), Waiter(), Waiter())
    val cookList: MutableList<Cook> = mutableListOf(Cook(SpecialtyType.DESSERT), Cook(SpecialtyType.ENTRY),Cook(SpecialtyType.ENTRY))

    init {
        startRestaurantMenu()
        createGroupQueue()
    }

    private fun createGroupQueue() {
        timeArrivalsClientList.indices.forEach{
            groupQueue.push(CostumerGroup(timeArrivalsClientList[it]))
        }
    }

    private fun startRestaurantMenu() {
        entreePlateList.add(EntreePlate("Causa de atún", Time(0, 7, 10), Time(0, 5, 0), 13.000))
        entreePlateList.add(EntreePlate("Chicharrón de calamar", Time(0, 4, 0), Time(0, 6, 0), 11.000))
        mainCourseList.add(MainCourse("Aguadito norteño", Time(0, 27, 12), Time(0, 11, 23), 33.800))
        mainCourseList.add(MainCourse("Chupe de langostinos", Time(0, 32, 0), Time(0, 15, 19), 35.200))
        dessertPlateList.add(DessertPlate("Chocotejas", Time(0, 8, 7), Time(0, 11, 34), 5.000))
        dessertPlateList.add(DessertPlate("Arroz con leche", Time(0, 7, 12), Time(0, 13, 19), 3.600))
    }

    fun poolToGroupQueue() {
        groupQueue.pool()
    }

    fun addOrderQueue(costumerGroup: CostumerGroup) {
        val groupId = costumerGroup.customerGroupId
        val clientList = costumerGroup.customerList
        val order = Order()
        var item: OrderItem? = null
        var code: Int
        var ratingList: ArrayList<Rating>
        clientList.filterNotNull().forEach { myClient ->
            ratingList = myClient.ratingList
            ratingList.forEachIndexed { index, qualification ->
                code = qualification.getcode()
                if (code != -1) {
                    item = getItem(item, code, ratingList, index, groupId)
                    order.addItem(item)
                }
            }
        }
        orderQueue.push(order)
    }

    private fun getItem(
        item: OrderItem?,
        code: Int,
        ratingList: ArrayList<Rating>,
        index: Int,
        idGroup: Long
    ): OrderItem? {
        var item = item
        var plate: Plate
        when (ratingList!![index].type) {
            RatingType.ENTRY -> {
                plate = entreePlateList[code]
                item = OrderItem(plate, SpecialtyType.ENTRY, idGroup)
            }
            RatingType.MAIN_COURSE -> {
                plate = mainCourseList[code]
                item = OrderItem(plate, SpecialtyType.MAIN_COURSE, idGroup)
            }
            RatingType.DESSERT -> {
                plate = dessertPlateList[code]
                item = OrderItem(plate, SpecialtyType.DESSERT, idGroup)
            }
        }
        return item
    }

    fun cookPlate(cook: Cook, orderItem: OrderItem?, currentTime: Time?) {
        cookList[cook.cookId.toInt()].cookPlate(orderItem, currentTime)
    }

    fun setDepartureTimeGroup(currentTime: Time, orderItem: OrderItem) {
        currentTime.addTime(orderItem.plate.consumptionTime)
        for (i in invoiceList.indices) {
            if (invoiceList[i].costumerGroup?.customerGroupId == orderItem.idGroup) {
                invoiceList[i].costumerGroup?.departureTime = currentTime
            }
        }
    }

    fun addInvoiceList(invoice: Invoice) {
        invoiceList.add(invoice)
    }

    fun pushPaymentQueue(costumerGroup: CostumerGroup?) {
        paymentQueue.push(costumerGroup!!)
    }

    fun pushPaymentPriorityQueue(costumerGroup: CostumerGroup?) {
        paymentPriorityQueue.push(costumerGroup!!)
    }

    fun poolOrderItem() {
        orderQueue.peek()?.orderItemQueue?.pool()
        if (orderQueue.peek()?.orderItemQueue?.isEmpty == true) {
            orderQueue.pool()
        }
    }

    fun deleteorder() {
        if (!orderQueue.isEmpty) {
            if (orderQueue.peek()?.orderItemQueue?.isEmpty == true) {
                orderQueue.pool()
            }
        }
    }

    // ================== Reportes ===============
    val paytmentType: HashMap<PaymentType, Int>
        get() {
            var countPaymentTypeCard = 0
            var countPaymentTypeEfecty = 0
            for (actualInvoice in invoiceList) {
                val paymentType = actualInvoice.paymentType
                if (paymentType == PaymentType.CREDIT_CARD) {
                    countPaymentTypeCard++
                } else {
                    countPaymentTypeEfecty++
                }
            }
            val paymentsList = HashMap<PaymentType, Int>()
            paymentsList[PaymentType.CASH] = countPaymentTypeEfecty
            paymentsList[PaymentType.CREDIT_CARD] = countPaymentTypeCard
            return paymentsList
        }

    /**
     * Ingresos en bruto del restaurante en cada una de las muestras
     * @return
     */
    val grossIncome: Int
        get() {
            var countEntreePlate = 0
            var countMainPlate = 0
            var countDessertPlate = 0
            for (actualInvoice in invoiceList) {
                val costumerGroup = actualInvoice.costumerGroup
                val clientsList = costumerGroup?.customerList
                for (actualClient in clientsList!!) {
                    val qualificationList = actualClient?.ratingList
                    for (actualQuialification in qualificationList!!) {
                        val code = actualQuialification!!.getcode()
                        val qualificationType = actualQuialification.type
                        if (code != -1) {
                            if (qualificationType == RatingType.ENTRY) {
                                val entreePlate = entreePlateList[code]
                                countEntreePlate += entreePlate.cost.toInt()
                            } else if (qualificationType == RatingType.MAIN_COURSE) {
                                val mainPlate = mainCourseList[code]
                                countMainPlate += mainPlate.cost.toInt()
                            } else if (qualificationType == RatingType.DESSERT) {
                                val dessertPlate = dessertPlateList[code]
                                countDessertPlate += dessertPlate.cost.toInt()
                            }
                        }
                    }
                }
            }
            return countEntreePlate + countMainPlate + countDessertPlate
        }

    /**
     * Mesero con mayor calificaci�n diaria durante cada muestra
     * @return
     */
    val waiterBestCalificated: Waiter
        get() {
            val countScoreWaiterList = IntArray(waiterList.size)
            val countWaiterList = IntArray(waiterList.size)
            for (i in invoiceList.indices) {
                val actualInvoice = invoiceList[i]
                val quialification = actualInvoice.waiterRating
                if (quialification != null) {
                    val score = quialification.score
                    val code = quialification.getcode()
                    if (code != -1) {
                        countScoreWaiterList[code] += score
                        countWaiterList[code]++
                    }
                }
            }
            val averageWaiter = getAverageScore(countScoreWaiterList, countWaiterList)
            val coordenateWaiter = getBestCoordenate(averageWaiter)
            return waiterList[coordenateWaiter]
        }

    /**
     * Entrada, plato y Postre mejor vendidos
     * @return
     */
    val entreePlateMainPlateAndDessertBestCalificated: Array<Plate>
        get() {
            val countScoreEntreePlateList = IntArray(entreePlateList.size)
            val countEntreePlateList = IntArray(entreePlateList.size)
            val countScoreMainPlateList = IntArray(mainCourseList.size)
            val countMainPlateList = IntArray(mainCourseList.size)
            val countScoreDessertPlateList = IntArray(dessertPlateList.size)
            val countDessertPlateList = IntArray(dessertPlateList.size)
            for (actualInvoice in invoiceList) {
                val costumerGroup = actualInvoice.costumerGroup
                val clientsList = costumerGroup?.customerList
                for (actualClient in clientsList!!) {
                    val qualificationList = actualClient?.ratingList
                    for (actualQuialification in qualificationList!!) {
                        val code = actualQuialification!!.getcode()
                        val score = actualQuialification.score
                        val qualificationType = actualQuialification.type
                        if (code != -1) {
                            if (qualificationType == RatingType.ENTRY) {
                                countScoreEntreePlateList[code] += score
                                countEntreePlateList[code]++
                            } else if (qualificationType == RatingType.MAIN_COURSE) {
                                countScoreMainPlateList[code] += score
                                countMainPlateList[code]++
                            } else if (qualificationType == RatingType.DESSERT) {
                                countScoreDessertPlateList[code] += score
                                countDessertPlateList[code]++
                            }
                        }
                    }
                }
            }
            val averageEntreePlate = getAverageScore(countScoreEntreePlateList, countEntreePlateList)
            val averageMainPlate = getAverageScore(countScoreMainPlateList, countMainPlateList)
            val averageDessertPlate = getAverageScore(countScoreDessertPlateList, countDessertPlateList)
            val coordenateEntreePlate = getBestCoordenate(averageEntreePlate)
            val coordenateMainPlate = getBestCoordenate(averageMainPlate)
            val coordenateDessertPlate = getBestCoordenate(averageDessertPlate)
            val entreePlate = entreePlateList[coordenateEntreePlate]
            val mainPlate = mainCourseList[coordenateMainPlate]
            val dessertPlate = dessertPlateList[coordenateDessertPlate]
            return arrayOf(entreePlate, mainPlate, dessertPlate)
        }

    val entreePlateMainPlateAndDessertBestSeller: Array<Plate>
        get() {
            val countEntreePlateList = IntArray(entreePlateList.size)
            val countMainPlateList = IntArray(mainCourseList.size)
            val countDessertPlateList = IntArray(dessertPlateList.size)
            for (actualInvoice in invoiceList) {
                val costumerGroup = actualInvoice.costumerGroup
                val clientsList = costumerGroup?.customerList
                for (actualClient in clientsList!!) {
                    val qualificationList = actualClient?.ratingList
                    for (actualQuialification in qualificationList!!) {
                        val code = actualQuialification!!.getcode()
                        val qualificationType = actualQuialification.type
                        if (code != -1) {
                            if (qualificationType == RatingType.ENTRY) {
                                countEntreePlateList[code]++
                            } else if (qualificationType == RatingType.MAIN_COURSE) {
                                countMainPlateList[code]++
                            } else if (qualificationType == RatingType.DESSERT) {
                                countDessertPlateList[code]++
                            }
                        }
                    }
                }
            }
            val coordenateEntreePlate = getBestCoordenate(countEntreePlateList)
            val coordenateMainPlate = getBestCoordenate(countMainPlateList)
            val coordenateDessertPlate = getBestCoordenate(countDessertPlateList)
            val entreePlate = entreePlateList[coordenateEntreePlate]
            val mainPlate = mainCourseList[coordenateMainPlate]
            val dessertPlate = dessertPlateList[coordenateDessertPlate]
            return arrayOf(entreePlate, mainPlate, dessertPlate)
        }

    private fun getBestCoordenate(countEntreePlateList: IntArray): Int {
        var actual = 0
        var position = 0
        for (i in countEntreePlateList.indices) {
            if (countEntreePlateList[i] > actual) {
                actual = countEntreePlateList[i]
                position = i
            }
        }
        return position
    }

    private fun getAverageScore(scoreList: IntArray, countList: IntArray): IntArray {
        val averageList = IntArray(scoreList.size)
        for (i in scoreList.indices) {
            if (countList[i] != 0) {
                averageList[i] = scoreList[i] / countList[i]
            }
        }
        return averageList
    }

    companion object {
        private const val WAITER_NUMBER = 3

        private const val WEEKS_TO_SIMULATE = 3
        private const val DAYS_PER_WEEKS = 7
    }
}