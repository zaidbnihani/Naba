package com.nba.plus.data.remote

/**
 * إعدادات الاتصال بالخادم.
 *
 * TODO(مهم): استبدل BASE_URL برابط Supabase Edge Function الخاص بك،
 * مثال: "https://abcdefgh.supabase.co/functions/v1/"
 * الدالة توكيل (proxy) لـ NewsData.io ويجب ألا يُضمَّن مفتاح NewsData.io
 * داخل التطبيق إطلاقًا — انظر README لتفاصيل عقد الطلب/الاستجابة.
 *
 * طالما لم يُستبدل العنوان الوهمي، يعمل التطبيق في «الوضع التجريبي»
 * ببيانات عربية مدمجة (assets/mock) وتتعطل تلقائيًا عند الاستبدال.
 */
object ApiConfig {
    const val BASE_URL = "https://YOUR-SUPABASE-PROJECT.supabase.co/functions/v1/"
    const val PLACEHOLDER_HOST = "YOUR-SUPABASE-PROJECT"

    /** true طالما لم يُضبط رابط الدالة الحقيقية بعد. */
    val isDemoMode: Boolean get() = BASE_URL.contains(PLACEHOLDER_HOST)
}
