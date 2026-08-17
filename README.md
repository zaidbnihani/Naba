# نبا+ — تطبيق أخبار عربي (Kotlin + Jetpack Compose)

تطبيق أخبار عربي احترافي (RTL، ثيم داكن افتراضي) يجمع أخبارًا عربية وأردنية وعالمية عبر **Supabase Edge Function** توكّل طلباتها إلى NewsData.io، مع مفضلة ومتابعات وبحث وترقيم لانهائي وإشعارات عاجلة عبر FCM، وطبقة تخصيص بالذكاء الاصطناعي مهيأة للتوصيل لاحقًا.

> ⚠️ **الوضع التجريبي**: طالما لم يُستبدل رابط الدالة الطرفية، يعمل التطبيق ببيانات عربية مدمجة (45 مقالًا، 12 مصدرًا) بكامل الوظائف — التغذية، الترقيم، البحث، المتابعات، المفضلة. استبدال الرابط (أدناه) يحوّل التطبيق تلقائيًا إلى بياناتك الحقيقية دون أي تعديل آخر.

---

## 1) متطلبات التشغيل

| الأداة | الإصدار الموصى به |
|---|---|
| Android Studio | Ladybug (2024.2.1) أو أحدث |
| JDK | 17 (مضمّن مع Android Studio) |
| Android SDK | compileSdk 35 / minSdk 26 |
| Gradle | 8.9 عبر الـ Wrapper المضمّن |

## 2) فتح المشروع والبناء

1. افتح Android Studio → **Open** → اختر مجلد `نبا+` (المجلد الجذر الذي يحوي `settings.gradle.kts`).
2. انتظر مزامنة Gradle (سيُنزّل Wrapper إصدار 8.9 تلقائيًا).
3. شغّل على محاكي أو جهاز (API 26+): زر **Run**.

> إن ظهرت مشكلة ترميز بسبب المسار العربي، انسخ المشروع إلى مسار ASCII مثل `C:\projects\nbaplus`.

## 3) ربط Supabase Edge Function (الأهم)

التطبيق **لا يستدعي NewsData.io مباشرة أبدًا** ولا يحوي أي مفتاح API لها.

### 3.1 ضع الرابط
افتح `app/src/main/java/com/nba/plus/data/remote/ApiConfig.kt`:

```kotlin
object ApiConfig {
    // TODO: استبدل هذا العنوان برابط Supabase Edge Function الخاص بك.
    const val BASE_URL = "https://YOUR-SUPABASE-PROJECT.supabase.co/functions/v1/"
    ...
}
```

استبدل `YOUR-SUPABASE-PROJECT` بمعرّف مشروعك. انتهى — الاعتراض التجريبي `MockNewsInterceptor` يتوقف تلقائيًا بمجرد تغيّر الرابط.

### 3.2 عقد الدالة (Endpoint contract)

**`GET {BASE_URL}/news`** — معاملات الاستعلام:

| المعامل | مثال | ملاحظات |
|---|---|---|
| `page` | `1` | ترقيم صفحات (حجم الصفحة 10 من جهة العميل) |
| `category` | `politics,sports` | فئات متعددة مفصولة بفواصل (اختياري) |
| `source` | `petra,kooora` | مصادر متعددة مفصولة بفواصل (اختياري) |
| `q` | `الأردن` | بحث نصي (اختياري) |
| `country` | `jo` | ثابت من العميل |
| `language` | `ar` | ثابت من العميل |

**الاستجابة المتوقعة** (مستوحاة من شكل NewsData.io):

```json
{
  "status": "success",
  "totalResults": 45,
  "nextPage": 2,
  "results": [
    {
      "article_id": "abc123",
      "title": "عنوان الخبر",
      "description": "مقدمة قصيرة",
      "content": "النص الكامل. الفقرات تفصل بينها \\n\\n",
      "link": "https://source.com/article",
      "image_url": "https://cdn/800/500.jpg",
      "source_id": "petra",
      "source_name": "بترا",
      "source_icon": "https://cdn/icon.png",
      "category": ["politics"],
      "pubDate": "2026-08-17 09:30:00",
      "is_breaking": true,
      "popularity": 9800,
      "like_count": 4200,
      "comment_count": 650
    }
  ]
}
```

مثال تنفيذ الدالة (Deno) داخل Supabase — مرر مفتاح NewsData.io كـ **secret** ولا تضعه في التطبيق:

```ts
import { serve } from "https://deno.land/std/http/server.ts";

serve(async (req) => {
  const url = new URL(req.url);
  const params = url.searchParams;
  const qs = new URLSearchParams({
    page: params.get("page") ?? "1",
    language: "ar",
    ...(["category", "source", "q", "country"].reduce((a, k) => {
      if (params.get(k)) a[k] = params.get(k)!;
      return a;
    }, {} as Record<string, string>)),
  });

  const upstream = await fetch(
    `https://newsdata.io/api/1/news?apikey=${Deno.env.get("NEWSDATA_API_KEY")!}&${qs}`
  );
  const data = await upstream.json();

  // أعِد التشكيل للعقد أعلاه (خُذ أول فئة من المصفوفة وهكذا)
  return new Response(JSON.stringify({
    status: "success",
    totalResults: data.totalResults,
    nextPage: data.nextPage ? Number(data.nextPage) : null,
    results: (data.results ?? []).map((r: any) => ({
      article_id: r.article_id,
      title: r.title ?? "",
      description: r.description ?? "",
      content: r.content ?? "",
      link: r.link ?? "",
      image_url: r.image_url ?? null,
      source_id: r.source_id ?? "",
      source_name: r.source_id ?? r.source?.[0] ?? "",
      source_icon: null,
      category: r.category ?? [],
      pubDate: r.pubDate ?? null,
      is_breaking: false,
      popularity: 0,
      like_count: 0,
      comment_count: 0,
    })),
  }), {
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  };
});
```

**`GET {BASE_URL}/sources`** — قائمة المصادر:

```json
{
  "sources": [
    {
      "source_id": "petra",
      "name": "وكالة الأنباء الأردنية (بترا)",
      "icon_url": "https://cdn/icon.png",
      "banner_url": "https://cdn/banner.jpg",
      "description": "الوكالة الرسمية للأخبار",
      "category": "politics",
      "followers": 890000
    }
  ]
}
```

### 3.3 المصادقة والمزامنة (GoTrue + Postgrest)

افتح `app/src/main/java/com/nba/plus/data/supabase/SupabaseSync.kt` وضع القيم:

```kotlin
object SupabaseConfig {
    const val SUPABASE_URL = "https://YOUR-SUPABASE-PROJECT.supabase.co"
    const val SUPABASE_ANON_KEY = "YOUR-SUPABASE-ANON-KEY"
    ...
}
```

ثم نفّذ هذا SQL في SQL Editor داخل Supabase:

```sql
create table if not exists public.followed_sources (
  user_id uuid not null references auth.users(id) on delete cascade,
  source_id text not null,
  followed_at timestamptz default now(),
  primary key (user_id, source_id)
);

create table if not exists public.followed_categories (
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id text not null,
  followed_at timestamptz default now(),
  primary key (user_id, category_id)
);

create table if not exists public.saved_articles (
  user_id uuid not null references auth.users(id) on delete cascade,
  article_id text not null,
  title text not null,
  url text not null,
  image_url text,
  saved_at bigint not null,
  primary key (user_id, article_id)
);

alter table public.followed_sources enable row level security;
alter table public.followed_categories enable row level security;
alter table public.saved_articles enable row level security;

create policy "own rows" on public.followed_sources
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own rows" on public.followed_categories
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own rows" on public.saved_articles
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
```

ومن **Authentication ← Providers** فعّل:
- **Anonymous sign-ins** (الدخول المجهول الافتراضي عند أول إقلاع).
- **Email** (لتسجيل الدخول/إنشاء الحساب من شاشة «حسابي»).

**ملاحظات**:
- كل مزامنات Supabase مغلّفة بأمان؛ الفشل أو غياب الإعداد لا يعطل أي ميزة محلية.
- حذف الحساب من جهة العميل يتطلب دالة طرفية بصلاحيات admin (service_role). أنشئ دالة `delete-user` واستدعها من `AuthRepositoryImpl.deleteAccount()` (يوجد TODO في الكود).

## 4) ربط Firebase (إشعارات FCM)

1. أنشئ مشروعًا في [Firebase Console](https://console.firebase.google.com) وأضف تطبيق Android بمعرّف الحزمة **`com.nba.plus`**.
2. نزّل `google-services.json` وضعه داخل مجلد **`app/`** (يوجد ملف `google-services.json.example` للاسترشاد).
3. أعد البناء — إضافة `google-services` تُطبَّق تلقائيًا عند وجود الملف فقط.
4. للإرسال: من Console ← Cloud Messaging، أرسل **رسالة بيانات (Data message)** بالحقول:
   - `title` — عنوان التنبيه
   - `body` — النص
   - `channel` — `breaking` | `top_stories` | `sports`

تُدار الاشتراكات تلقائيًا عبر مواضيع FCM بحسب مفاتيح التنبيهات في «حسابي»: `breaking`، `top_stories`، `sports`، `match_results`.

## 5) بنية المشروع

```
app/src/main/java/com/nba/plus/
├─ NbaApp.kt, MainActivity.kt          # نقطة الإقلاع (RTL + Splash + الثيم)
├─ di/                                  # وحدات Hilt (شبكة، Room، Supabase، DataStore…)
├─ data/
│  ├─ remote/                           # Retrofit + عقد الدالة + الوضع التجريبي
│  ├─ local/                            # Room (كاش/محفوظات/متابعات/dedup/بحثات)
│  ├─ preferences/                      # DataStore للتفضيلات
│  ├─ supabase/                         # مزامنة GoTrue + Postgrest
│  └─ repository/                       # تطبيقات مستودعات المجال
├─ domain/
│  ├─ model/                            # Article, Source, UserPreferences…
│  ├─ repository/                       # واجهات (News, Saved, Auth, Personalization…)
│  ├─ usecase/                          # GetFeed, Search, ToggleSave
│  └─ util/                             # TitleNormalizer + DedupDetector
├─ push/                                # FCM: خدمة الرسائل + القنوات + مزامنة المواضيع
└─ ui/
   ├─ theme/                            # ألوان اللقطات، خط Cairo المتغيّر، Shimmer
   ├─ components/                       # NewsCard, CategoryCard, Skeletons…
   ├─ navigation/                       # المسارات + الشريط السفلي
   └─ screens/                          # 14 شاشة (تغذية، مصادر، مجالات، حسابي…)
```

## 6) خرائط الميزات

| الميزة | أين تجدها |
|---|---|
| تغذية بترقيم + إزالة تكرار | `feed/LatestNewsScreen` + `NewsRepositoryImpl.FeedPagingSource` + `DedupDetector` |
| سحب للتحديث | `PullToRefreshBox` في شاشات التغذية |
| المفضلة (Room + Supabase) | `SavedArticlesRepositoryImpl` |
| مشاركة أصلية | `ui/util/ShareUtils.kt` |
| متابعة مصادر/فئات | `SourcesRepositoryImpl` / `CategoriesRepositoryImpl` |
| إشعارات عاجلة بمفاتيح دقيقة | `push/` + مفاتيح شاشة «حسابي» |
| ثيم داكن/فاتح + حجم خط | `UserPreferencesDataStore` + `MainActivity` |
| هياكل تحميل Shimmer | `ui/components/StateComponents.kt` |
| الذكاء الاصطناعي (Scaffold) | واجهة `PersonalizationRepository` + `MockPersonalizationRepository` |

## 7) تغيير اسم الحزمة

ابحث واستبدل `com.nba.plus` في: `app/build.gradle.kts` (namespace + applicationId)، مجلد `java/com/nba/plus`، و`package_name` في ملف Firebase، ثم أعد المزامنة.

## 8) ملاحظات للمطور

- **الوضع التجريبي** يُحدَّد من `ApiConfig.isDemoMode` (وجود قيمة placeholder في BASE_URL). لا حاجة لأي مفتاح تبديل يدوي.
- إزالة التكرار تعمل عبر الجلسات (جدول `seen_articles`) وتطبّع العناوين عربيًا (تشكيل/همزات/تاء مربوطة) مع تشابه Jaccard ≥ 0.75.
- كاش Room يعرض آخر الأخبار دون اتصال مع شريط «عرض من الكاش».
- اختبارات وحدة: `./gradlew test` أو من Android Studio (TitleNormalizer/DedupDetector).
- minify معطّل في release افتراضيًا؛ قواعد ProGuard جاهزة في `app/proguard-rules.pro` عند التفعيل.
