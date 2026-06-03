#!/usr/bin/env python3
"""
Full seeder: 10+ users, produk per seller, 120+ transaksi
Jalankan: python3 seeds/seed_full.py
"""
import subprocess, sys

MYSQL = "/Applications/MAMP/Library/bin/mysql80/bin/mysql"
ARGS  = ["-u","root","-proot","-P","8889",
         "-S","/Applications/MAMP/tmp/mysql/mysql.sock","db_marketplace"]

# Bcrypt hash dari password "password123" (cost 12)
PWD = "$2y$12$r1IITjj4LJlZir6uo2a38.i7TOlGDn1FyHaYf5vUdlRHNBUML.f8y"

def run(sql):
    r = subprocess.run([MYSQL]+ARGS, input=sql, capture_output=True, text=True)
    err = r.stderr.replace("mysql: [Warning] Using a password on the command line interface can be insecure.\n","")
    if r.returncode != 0 and err.strip():
        print("SQL ERROR:", err.strip()); sys.exit(1)
    return r.stdout

# ─── 1. USERS BARU (id 5-12) ──────────────────────────────────────────────────
NEW_USERS_SQL = f"""
INSERT IGNORE INTO users
  (id, name, email, phone, role, shop_name, address, password, email_verified_at, created_at, updated_at)
VALUES
(5,  'Budi Santoso',   'budi.santoso@gmail.com',   '08123456780', 'buyer',  NULL,
  'Jl. Cendrawasih No.12, Makassar', '{PWD}', NOW(), '2025-10-01 08:00:00', '2025-10-01 08:00:00'),
(6,  'Siti Rahayu',    'siti.rahayu@gmail.com',    '08234567891', 'buyer',  NULL,
  'Jl. Veteran Selatan No.5, Makassar', '{PWD}', NOW(), '2025-10-05 09:00:00', '2025-10-05 09:00:00'),
(7,  'Ahmad Fauzi',    'ahmad.fauzi@gmail.com',    '08345678902', 'buyer',  NULL,
  'Jl. Hertasning No.88, Makassar', '{PWD}', NOW(), '2025-10-10 10:00:00', '2025-10-10 10:00:00'),
(8,  'Dewi Lestari',   'dewi.lestari@gmail.com',   '08456789013', 'buyer',  NULL,
  'Jl. Rappocini Raya No.33, Makassar', '{PWD}', NOW(), '2025-10-15 11:00:00', '2025-10-15 11:00:00'),
(9,  'Rina Wahyuni',   'rina.wahyuni@gmail.com',   '08567890124', 'buyer',  NULL,
  'Jl. Tamalate No.21, Makassar', '{PWD}', NOW(), '2025-10-20 12:00:00', '2025-10-20 12:00:00'),
(10, 'Hendra Gunawan', 'hendra.toko@gmail.com',    '08678901235', 'seller', 'Toko Elektronik Maju',
  'Jl. Urip Sumoharjo No.55, Makassar', '{PWD}', NOW(), '2025-09-01 08:00:00', '2025-09-01 08:00:00'),
(11, 'Nisa Amalia',    'nisa.butik@gmail.com',     '08789012346', 'seller', 'Butik Fashion Nisa',
  'Jl. AP Pettarani No.17, Makassar', '{PWD}', NOW(), '2025-09-05 09:00:00', '2025-09-05 09:00:00'),
(12, 'Rudi Hartono',   'rudi.furniture@gmail.com', '08890123457', 'seller', 'Furniture Indah Store',
  'Jl. Boulevard No.9, Makassar', '{PWD}', NOW(), '2025-09-10 10:00:00', '2025-09-10 10:00:00');
"""

# ─── 2. PRODUK BARU ────────────────────────────────────────────────────────────
# seller 4 (Test / Toko tedt) → produk 12-14
# seller 10 (Hendra)          → produk 15-19
# seller 11 (Nisa)            → produk 20-24
# seller 12 (Rudi)            → produk 25-29
NEW_PRODUCTS_SQL = """
INSERT IGNORE INTO products
  (id, user_id, category_id, name, description, price, stock, `condition`,
   weight, image, location, latitude, longitude, created_at, updated_at)
VALUES
-- Seller 4
(12, 4, 1, 'Powerbank 20000mAh',
 'Powerbank kapasitas besar 20000mAh dual USB, cocok untuk perjalanan jauh.',
 150000.00, 10, 'new', 300, 'products/powerbank.jpg', 'Makassar',
 -5.14753, 119.43248, '2025-09-15 08:00:00', '2025-09-15 08:00:00'),
(13, 4, 1, 'Earphone Wired',
 'Earphone kabel dengan suara jernih, jack 3.5mm, cocok untuk semua HP.',
 45000.00, 20, 'new', 80, 'products/earphone.jpg', 'Makassar',
 -5.14753, 119.43248, '2025-09-15 09:00:00', '2025-09-15 09:00:00'),
(14, 4, 6, 'Casing HP Universal',
 'Casing HP bahan silikon lembut, tersedia untuk berbagai tipe HP.',
 35000.00, 30, 'new', 50, 'products/casing.jpg', 'Makassar',
 -5.14753, 119.43248, '2025-09-15 10:00:00', '2025-09-15 10:00:00'),
-- Seller 10
(15, 10, 1, 'Headset Gaming RGB',
 'Headset gaming dengan lampu RGB, suara surround 7.1, mikrofon noise cancelling.',
 285000.00, 15, 'new', 400, 'products/headset_gaming.jpg', 'Makassar',
 -5.13000, 119.42000, '2025-09-20 08:00:00', '2025-09-20 08:00:00'),
(16, 10, 1, 'Power Bank 10000mAh',
 'Power bank slim 10000mAh, fast charging 18W, port USB-A dan USB-C.',
 120000.00, 25, 'new', 250, 'products/powerbank10k.jpg', 'Makassar',
 -5.13000, 119.42000, '2025-09-20 09:00:00', '2025-09-20 09:00:00'),
(17, 10, 1, 'Charger Fast Charging 65W',
 'Charger GaN 65W, compact, support multi device, aman untuk laptop dan HP.',
 95000.00, 20, 'new', 150, 'products/charger65w.jpg', 'Makassar',
 -5.13000, 119.42000, '2025-09-20 10:00:00', '2025-09-20 10:00:00'),
(18, 10, 1, 'Mouse Wireless Silent',
 'Mouse wireless 2.4GHz, klik senyap, ergonomis, daya tahan baterai 12 bulan.',
 175000.00, 18, 'new', 120, 'products/mouse_wireless.jpg', 'Makassar',
 -5.13000, 119.42000, '2025-09-20 11:00:00', '2025-09-20 11:00:00'),
(19, 10, 1, 'Speaker Bluetooth Mini',
 'Speaker Bluetooth portabel, waterproof IPX5, baterai 8 jam, suara bass kuat.',
 210000.00, 12, 'new', 350, 'products/speaker_bt.jpg', 'Makassar',
 -5.13000, 119.42000, '2025-09-20 12:00:00', '2025-09-20 12:00:00'),
-- Seller 11
(20, 11, 3, 'Kaos Polos Pria',
 'Kaos polos bahan cotton combed 30s, tersedia berbagai warna dan ukuran S-XXL.',
 65000.00, 50, 'new', 200, 'products/kaos_polos.jpg', 'Makassar',
 -5.16000, 119.44000, '2025-09-25 08:00:00', '2025-09-25 08:00:00'),
(21, 11, 3, 'Celana Jogger Pria',
 'Celana jogger bahan fleece tebal, nyaman untuk olahraga dan santai.',
 115000.00, 30, 'new', 400, 'products/celana_jogger.jpg', 'Makassar',
 -5.16000, 119.44000, '2025-09-25 09:00:00', '2025-09-25 09:00:00'),
(22, 11, 3, 'Jaket Hoodie Unisex',
 'Hoodie distro premium, bahan tebal anti angin, tersedia ukuran S-XXL.',
 250000.00, 20, 'new', 600, 'products/hoodie.jpg', 'Makassar',
 -5.16000, 119.44000, '2025-09-25 10:00:00', '2025-09-25 10:00:00'),
(23, 11, 3, 'Kemeja Batik Modern',
 'Kemeja batik motif modern, bahan katun halus, cocok untuk formal dan casual.',
 185000.00, 25, 'new', 300, 'products/batik.jpg', 'Makassar',
 -5.16000, 119.44000, '2025-09-25 11:00:00', '2025-09-25 11:00:00'),
(24, 11, 2, 'Dress Casual Wanita',
 'Dress casual wanita bahan rayon adem, motif floral trendy.',
 145000.00, 20, 'new', 250, 'products/dress_casual.jpg', 'Makassar',
 -5.16000, 119.44000, '2025-09-25 12:00:00', '2025-09-25 12:00:00'),
-- Seller 12
(25, 12, 5, 'Rak Buku 5 Susun',
 'Rak buku kayu jati 5 susun, desain minimalis, kuat dan tahan lama.',
 320000.00, 8, 'new', 8000, 'products/rak_buku.jpg', 'Makassar',
 -5.18000, 119.46000, '2025-10-01 08:00:00', '2025-10-01 08:00:00'),
(26, 12, 5, 'Meja Kerja Minimalis',
 'Meja kerja minimalis dengan laci, cocok untuk WFH dan belajar.',
 650000.00, 5, 'new', 12000, 'products/meja_kerja.jpg', 'Makassar',
 -5.18000, 119.46000, '2025-10-01 09:00:00', '2025-10-01 09:00:00'),
(27, 12, 5, 'Sofa 2 Seater',
 'Sofa 2 dudukan bahan beludru premium, rangka kayu solid, nyaman dan elegan.',
 1850000.00, 3, 'new', 25000, 'products/sofa.jpg', 'Makassar',
 -5.18000, 119.46000, '2025-10-01 10:00:00', '2025-10-01 10:00:00'),
(28, 12, 5, 'Meja Makan 4 Kursi',
 'Set meja makan 4 kursi kayu jepara, finishing natural, tahan lama.',
 2500000.00, 2, 'new', 35000, 'products/meja_makan.jpg', 'Makassar',
 -5.18000, 119.46000, '2025-10-01 11:00:00', '2025-10-01 11:00:00'),
(29, 12, 5, 'Lemari Pakaian Sliding',
 'Lemari pakaian pintu sliding cermin, 3 pintu, kapasitas besar.',
 1200000.00, 4, 'new', 40000, 'products/lemari_sliding.jpg', 'Makassar',
 -5.18000, 119.46000, '2025-10-01 12:00:00', '2025-10-01 12:00:00');
"""

# ─── Data referensi ────────────────────────────────────────────────────────────
# products per seller: {seller_id: [(prod_id, harga), ...]}
SELLER_PRODUCTS = {
    2:  [(5,85000),(6,185000),(8,25000),(9,96000),(10,850000),(11,500000)],
    4:  [(12,150000),(13,45000),(14,35000)],
    10: [(15,285000),(16,120000),(17,95000),(18,175000),(19,210000)],
    11: [(20,65000),(21,115000),(22,250000),(23,185000),(24,145000)],
    12: [(25,320000),(26,650000),(27,1850000),(28,2500000),(29,1200000)],
}

BUYERS = [3, 5, 6, 7, 8, 9]  # pembeli, budi, siti, ahmad, dewi, rina

PAYMENT_METHODS = [
    ("BCA Virtual Account","bank_bca"),
    ("Mandiri Virtual Account","bank_mandiri"),
    ("BNI Virtual Account","bank_bni"),
    ("GoPay","gopay"),
    ("DANA","dana"),
    ("OVO","ovo"),
]

ADDRESSES = [
    "Jl. Sudirman No. 10, Makassar, Sulawesi Selatan 90111",
    "Jl. Pettarani No. 5, Makassar, Sulawesi Selatan 90222",
    "Jl. AP. Pettarani Blok A No. 3, Gowa, Sulawesi Selatan 92116",
    "Jl. Perintis Kemerdekaan No. 99, Makassar, Sulawesi Selatan 90245",
    "Jl. Urip Sumoharjo No. 17, Makassar, Sulawesi Selatan 90232",
    "Jl. Hertasning No. 44, Makassar, Sulawesi Selatan 90220",
]

COURIERS = ["JNE","SICEPAT","JNT","TIKI","GRAB"]

STATUSES = [
    "waiting_payment","pending","processing",
    "shipped","received","completed","cancelled","payment_rejected",
]

SELLER_NOTES = {
    "payment_rejected": [
        "Bukti pembayaran tidak jelas, mohon upload ulang",
        "Nominal transfer tidak sesuai",
        "Transfer dari rekening berbeda",
        "Bukti pembayaran expired",
        "Foto bukti tidak terbaca",
    ],
    "cancelled": [
        "Stok habis",
        "Pembeli membatalkan pesanan",
        "Produk tidak tersedia saat ini",
        "Duplikat pesanan",
        "Penjual tidak bisa memenuhi pesanan",
    ],
}

# ─── 120 Template transaksi ────────────────────────────────────────────────────
# (seller_id, buyer_id, [(prod_id,qty),...], status, pay_idx,
#  courier, addr_idx, shipping_cost, discount, created, updated)
TRX = [
    # ── Seller 2 (penjual) ── 30 transaksi
    (2, 3,  [(5,1),(8,2)],          "completed",       0,"JNE",   0,15000,    0,"2025-11-01 08:00","2025-11-06 10:00"),
    (2, 5,  [(6,1),(9,1)],          "completed",       1,"SICEPAT",1,18000,20000,"2025-11-02 09:00","2025-11-07 11:00"),
    (2, 6,  [(9,2)],                "shipped",         3,"GRAB",  2,10000,    0,"2025-11-03 10:00","2025-11-05 09:00"),
    (2, 7,  [(5,2),(8,3)],          "processing",      4,"JNE",   3,12000,    0,"2025-11-04 11:00","2025-11-05 08:00"),
    (2, 8,  [(6,1),(8,1)],          "pending",         2,"TIKI",  0,20000,    0,"2025-11-05 12:00","2025-11-05 12:00"),
    (2, 9,  [(9,1)],                "waiting_payment", 5,"JNT",   1, 9000,    0,"2025-11-06 08:00","2025-11-06 08:00"),
    (2, 3,  [(5,1),(9,1),(8,2)],    "completed",       0,"JNE",   2,22000,30000,"2025-11-07 09:00","2025-11-14 10:00"),
    (2, 5,  [(8,4)],                "cancelled",       3,"GRAB",  3, 8000,    0,"2025-11-08 10:00","2025-11-09 09:00"),
    (2, 6,  [(6,2)],                "received",        1,"SICEPAT",0,16000,    0,"2025-11-09 11:00","2025-11-14 12:00"),
    (2, 7,  [(9,2),(5,1)],          "completed",       4,"JNT",   1,20000,    0,"2025-11-10 12:00","2025-11-17 11:00"),
    (2, 8,  [(5,1),(8,1)],          "payment_rejected",2,"TIKI",  2,14000,    0,"2025-11-11 08:00","2025-11-12 07:30"),
    (2, 9,  [(6,1),(9,1)],          "completed",       0,"JNE",   3,22000,50000,"2025-11-12 09:00","2025-11-19 10:00"),
    (2, 3,  [(8,3)],                "processing",      5,"GRAB",  0, 7000,    0,"2025-11-13 10:00","2025-11-14 09:00"),
    (2, 5,  [(9,1),(5,2)],          "shipped",         3,"SICEPAT",1,18000,    0,"2025-11-14 11:00","2025-11-16 10:00"),
    (2, 6,  [(6,1),(8,2),(9,1)],    "completed",       1,"JNE",   2,25000,40000,"2025-11-15 12:00","2025-11-22 11:00"),
    (2, 7,  [(5,1)],                "waiting_payment", 4,"JNT",   3, 9000,    0,"2025-11-16 08:00","2025-11-16 08:00"),
    (2, 8,  [(9,2),(8,1)],          "completed",       0,"JNE",   0,20000,    0,"2025-11-17 09:00","2025-11-24 12:00"),
    (2, 9,  [(6,1)],                "pending",         2,"GRAB",  1,15000,    0,"2025-11-18 10:00","2025-11-18 10:00"),
    (2, 3,  [(5,2),(9,1)],          "completed",       1,"SICEPAT",2,18000,    0,"2025-11-19 11:00","2025-11-26 10:00"),
    (2, 5,  [(8,2),(5,1)],          "cancelled",       5,"TIKI",  3,12000,    0,"2025-11-20 12:00","2025-11-21 09:00"),
    (2, 6,  [(9,1),(8,3)],          "received",        0,"JNE",   0,16000,    0,"2025-11-21 08:00","2025-11-26 10:00"),
    (2, 7,  [(6,2)],                "completed",       3,"JNT",   1,18000,60000,"2025-11-22 09:00","2025-11-29 11:00"),
    (2, 8,  [(5,2),(8,2)],          "processing",      4,"GRAB",  2,14000,    0,"2025-11-23 10:00","2025-11-24 09:00"),
    (2, 9,  [(9,1)],                "shipped",         1,"JNE",   3,12000,    0,"2025-11-24 11:00","2025-11-26 10:00"),
    (2, 3,  [(6,1),(9,1),(8,1)],    "completed",       0,"SICEPAT",0,22000,45000,"2025-11-25 12:00","2025-12-02 11:00"),
    (2, 5,  [(5,1),(8,1)],          "pending",         2,"TIKI",  1,12000,    0,"2025-11-26 08:00","2025-11-26 08:00"),
    (2, 6,  [(9,2),(5,1)],          "completed",       5,"JNT",   2,20000,    0,"2025-11-27 09:00","2025-12-04 10:00"),
    (2, 7,  [(8,4)],                "waiting_payment", 3,"GRAB",  3, 8000,    0,"2025-11-28 10:00","2025-11-28 10:00"),
    (2, 8,  [(6,1),(8,2)],          "completed",       0,"JNE",   0,20000,30000,"2025-11-29 11:00","2025-12-06 12:00"),
    (2, 9,  [(5,1),(9,1)],          "payment_rejected",1,"SICEPAT",1,16000,    0,"2025-11-30 12:00","2025-12-01 08:00"),
    # ── Seller 4 (Test) ── 20 transaksi
    (4, 3,  [(12,1),(13,1)],        "completed",       0,"JNE",   2,15000,    0,"2025-11-01 09:00","2025-11-06 11:00"),
    (4, 5,  [(14,2)],               "completed",       3,"GRAB",  3, 8000,    0,"2025-11-03 10:00","2025-11-08 12:00"),
    (4, 6,  [(12,1),(14,1)],        "shipped",         1,"SICEPAT",0,14000,    0,"2025-11-05 11:00","2025-11-07 10:00"),
    (4, 7,  [(13,3)],               "processing",      4,"JNT",   1,10000,    0,"2025-11-07 12:00","2025-11-08 09:00"),
    (4, 8,  [(12,2)],               "pending",         2,"TIKI",  2,18000,    0,"2025-11-09 08:00","2025-11-09 08:00"),
    (4, 9,  [(14,3),(13,1)],        "waiting_payment", 5,"JNE",   3,12000,    0,"2025-11-11 09:00","2025-11-11 09:00"),
    (4, 3,  [(13,2),(14,2)],        "completed",       0,"GRAB",  0,10000,    0,"2025-11-13 10:00","2025-11-18 11:00"),
    (4, 5,  [(12,1)],               "cancelled",       3,"SICEPAT",1,12000,    0,"2025-11-15 11:00","2025-11-16 09:00"),
    (4, 6,  [(14,4)],               "received",        1,"JNT",   2, 8000,    0,"2025-11-17 12:00","2025-11-22 10:00"),
    (4, 7,  [(12,1),(13,2)],        "completed",       4,"JNE",   3,16000,    0,"2025-11-19 08:00","2025-11-26 12:00"),
    (4, 8,  [(13,1),(14,1)],        "payment_rejected",2,"TIKI",  0,10000,    0,"2025-11-21 09:00","2025-11-22 08:00"),
    (4, 9,  [(12,1),(14,2)],        "completed",       5,"JNE",   1,15000,    0,"2025-11-23 10:00","2025-11-30 11:00"),
    (4, 3,  [(14,3)],               "processing",      0,"GRAB",  2, 8000,    0,"2025-11-25 11:00","2025-11-26 09:00"),
    (4, 5,  [(13,2)],               "shipped",         3,"SICEPAT",3,12000,    0,"2025-11-27 12:00","2025-11-29 10:00"),
    (4, 6,  [(12,1),(13,1),(14,1)], "completed",       1,"JNT",   0,16000,    0,"2025-11-29 08:00","2025-12-06 10:00"),
    (4, 7,  [(14,2)],               "waiting_payment", 4,"JNE",   1, 8000,    0,"2025-12-01 09:00","2025-12-01 09:00"),
    (4, 8,  [(12,1),(13,1)],        "completed",       2,"TIKI",  2,15000,    0,"2025-12-03 10:00","2025-12-10 11:00"),
    (4, 9,  [(14,4)],               "pending",         0,"GRAB",  3, 8000,    0,"2025-12-05 11:00","2025-12-05 11:00"),
    (4, 3,  [(13,2),(14,1)],        "completed",       5,"JNE",   0,14000,    0,"2025-12-07 12:00","2025-12-14 10:00"),
    (4, 5,  [(12,1)],               "cancelled",       3,"SICEPAT",1,12000,    0,"2025-12-09 08:00","2025-12-10 09:00"),
    # ── Seller 10 (Hendra Elektronik) ── 25 transaksi
    (10,3,  [(15,1)],               "completed",       0,"JNE",   2,18000,    0,"2025-11-02 08:00","2025-11-09 10:00"),
    (10,5,  [(16,1),(17,1)],        "completed",       1,"SICEPAT",3,15000,    0,"2025-11-04 09:00","2025-11-11 11:00"),
    (10,6,  [(18,1)],               "shipped",         3,"GRAB",  0,12000,    0,"2025-11-06 10:00","2025-11-08 09:00"),
    (10,7,  [(19,1),(16,1)],        "processing",      4,"JNT",   1,16000,    0,"2025-11-08 11:00","2025-11-09 08:00"),
    (10,8,  [(15,1),(17,2)],        "pending",         2,"JNE",   2,20000,    0,"2025-11-10 12:00","2025-11-10 12:00"),
    (10,9,  [(18,1),(19,1)],        "completed",       5,"TIKI",  3,18000,50000,"2025-11-12 08:00","2025-11-19 10:00"),
    (10,3,  [(16,2)],               "waiting_payment", 0,"GRAB",  0,12000,    0,"2025-11-14 09:00","2025-11-14 09:00"),
    (10,5,  [(15,1),(18,1)],        "completed",       3,"JNE",   1,22000,    0,"2025-11-16 10:00","2025-11-23 11:00"),
    (10,6,  [(17,1),(16,1)],        "received",        1,"SICEPAT",2,14000,    0,"2025-11-18 11:00","2025-11-23 10:00"),
    (10,7,  [(19,1)],               "completed",       4,"JNT",   3,15000,    0,"2025-11-20 12:00","2025-11-27 12:00"),
    (10,8,  [(15,1)],               "payment_rejected",2,"JNE",   0,18000,    0,"2025-11-22 08:00","2025-11-23 07:30"),
    (10,9,  [(16,1),(17,1),(18,1)], "completed",       0,"TIKI",  1,20000,    0,"2025-11-24 09:00","2025-12-01 10:00"),
    (10,3,  [(19,1),(16,1)],        "processing",      5,"GRAB",  2,16000,    0,"2025-11-26 10:00","2025-11-27 09:00"),
    (10,5,  [(15,1),(17,1)],        "shipped",         3,"JNE",   3,20000,    0,"2025-11-28 11:00","2025-11-30 10:00"),
    (10,6,  [(18,2)],               "completed",       1,"SICEPAT",0,18000,40000,"2025-11-30 12:00","2025-12-07 11:00"),
    (10,7,  [(16,1)],               "waiting_payment", 4,"JNT",   1,12000,    0,"2025-12-02 08:00","2025-12-02 08:00"),
    (10,8,  [(19,1),(15,1)],        "completed",       2,"JNE",   2,22000,    0,"2025-12-04 09:00","2025-12-11 10:00"),
    (10,9,  [(17,2)],               "pending",         0,"TIKI",  3,14000,    0,"2025-12-06 10:00","2025-12-06 10:00"),
    (10,3,  [(18,1),(16,1)],        "completed",       5,"GRAB",  0,18000,    0,"2025-12-08 11:00","2025-12-15 12:00"),
    (10,5,  [(15,1)],               "cancelled",       3,"JNE",   1,18000,    0,"2025-12-10 12:00","2025-12-11 09:00"),
    (10,6,  [(19,1),(17,1)],        "received",        1,"SICEPAT",2,16000,    0,"2025-12-12 08:00","2025-12-17 10:00"),
    (10,7,  [(16,2),(18,1)],        "completed",       4,"JNT",   3,20000,30000,"2025-12-14 09:00","2025-12-21 11:00"),
    (10,8,  [(15,1),(17,1)],        "processing",      2,"JNE",   0,18000,    0,"2025-12-16 10:00","2025-12-17 09:00"),
    (10,9,  [(19,2)],               "shipped",         0,"TIKI",  1,16000,    0,"2025-12-18 11:00","2025-12-20 10:00"),
    (10,3,  [(16,1),(17,1),(18,1)], "completed",       5,"GRAB",  2,20000,50000,"2025-12-20 12:00","2025-12-28 11:00"),
    # ── Seller 11 (Nisa Fashion) ── 25 transaksi
    (11,3,  [(20,2),(21,1)],        "completed",       0,"JNE",   3,16000,    0,"2025-11-02 10:00","2025-11-09 12:00"),
    (11,5,  [(22,1)],               "completed",       1,"SICEPAT",0,18000,    0,"2025-11-04 11:00","2025-11-11 10:00"),
    (11,6,  [(23,1),(24,1)],        "shipped",         3,"GRAB",  1,14000,    0,"2025-11-06 12:00","2025-11-08 11:00"),
    (11,7,  [(20,3)],               "processing",      4,"JNT",   2,10000,    0,"2025-11-08 08:00","2025-11-09 09:00"),
    (11,8,  [(21,1),(22,1)],        "pending",         2,"JNE",   3,20000,    0,"2025-11-10 09:00","2025-11-10 09:00"),
    (11,9,  [(24,2)],               "completed",       5,"TIKI",  0,14000,30000,"2025-11-12 10:00","2025-11-19 11:00"),
    (11,3,  [(23,1),(20,2)],        "waiting_payment", 0,"GRAB",  1,16000,    0,"2025-11-14 11:00","2025-11-14 11:00"),
    (11,5,  [(22,1),(21,1)],        "completed",       3,"JNE",   2,22000,    0,"2025-11-16 12:00","2025-11-23 10:00"),
    (11,6,  [(20,2),(24,1)],        "received",        1,"SICEPAT",3,16000,    0,"2025-11-18 08:00","2025-11-23 12:00"),
    (11,7,  [(21,2)],               "completed",       4,"JNT",   0,14000,    0,"2025-11-20 09:00","2025-11-27 11:00"),
    (11,8,  [(22,1)],               "payment_rejected",2,"JNE",   1,18000,    0,"2025-11-22 10:00","2025-11-23 08:00"),
    (11,9,  [(23,1),(24,1),(20,1)], "completed",       0,"TIKI",  2,18000,    0,"2025-11-24 11:00","2025-12-01 12:00"),
    (11,3,  [(21,1),(22,1)],        "processing",      5,"GRAB",  3,20000,    0,"2025-11-26 12:00","2025-11-27 11:00"),
    (11,5,  [(24,2)],               "shipped",         3,"JNE",   0,14000,    0,"2025-11-28 08:00","2025-11-30 09:00"),
    (11,6,  [(20,3),(21,1)],        "completed",       1,"SICEPAT",1,18000,35000,"2025-11-30 09:00","2025-12-07 10:00"),
    (11,7,  [(22,1)],               "waiting_payment", 4,"JNT",   2,18000,    0,"2025-12-02 10:00","2025-12-02 10:00"),
    (11,8,  [(23,1),(20,2)],        "completed",       2,"JNE",   3,16000,    0,"2025-12-04 11:00","2025-12-11 12:00"),
    (11,9,  [(24,1),(21,1)],        "pending",         0,"TIKI",  0,16000,    0,"2025-12-06 12:00","2025-12-06 12:00"),
    (11,3,  [(22,1),(23,1)],        "completed",       5,"GRAB",  1,20000,    0,"2025-12-08 08:00","2025-12-15 10:00"),
    (11,5,  [(20,2)],               "cancelled",       3,"JNE",   2,10000,    0,"2025-12-10 09:00","2025-12-11 08:00"),
    (11,6,  [(21,2),(24,1)],        "received",        1,"SICEPAT",3,18000,    0,"2025-12-12 10:00","2025-12-17 12:00"),
    (11,7,  [(22,1),(20,2)],        "completed",       4,"JNT",   0,20000,40000,"2025-12-14 11:00","2025-12-21 10:00"),
    (11,8,  [(23,1)],               "processing",      2,"JNE",   1,14000,    0,"2025-12-16 12:00","2025-12-17 11:00"),
    (11,9,  [(24,2),(21,1)],        "shipped",         0,"TIKI",  2,18000,    0,"2025-12-18 08:00","2025-12-20 09:00"),
    (11,3,  [(22,1),(23,1),(20,1)], "completed",       5,"GRAB",  3,20000,50000,"2025-12-20 09:00","2025-12-28 10:00"),
    # ── Seller 12 (Rudi Furniture) ── 20 transaksi
    (12,3,  [(25,1)],               "completed",       0,"JNE",   0,35000,    0,"2025-11-03 08:00","2025-11-12 10:00"),
    (12,5,  [(26,1)],               "completed",       1,"SICEPAT",1,45000,    0,"2025-11-05 09:00","2025-11-14 11:00"),
    (12,6,  [(27,1)],               "shipped",         3,"JNE",   2,60000,    0,"2025-11-07 10:00","2025-11-09 09:00"),
    (12,7,  [(25,1),(29,1)],        "processing",      4,"TIKI",  3,50000,    0,"2025-11-09 11:00","2025-11-10 09:00"),
    (12,8,  [(26,1)],               "pending",         2,"JNE",   0,45000,    0,"2025-11-11 12:00","2025-11-11 12:00"),
    (12,9,  [(28,1)],               "completed",       5,"SICEPAT",1,70000,200000,"2025-11-13 08:00","2025-11-24 10:00"),
    (12,3,  [(29,1)],               "waiting_payment", 0,"JNE",   2,55000,    0,"2025-11-15 09:00","2025-11-15 09:00"),
    (12,5,  [(25,1),(26,1)],        "received",        3,"TIKI",  3,55000,    0,"2025-11-17 10:00","2025-11-24 11:00"),
    (12,6,  [(27,1)],               "completed",       1,"JNE",   0,60000,100000,"2025-11-19 11:00","2025-11-30 12:00"),
    (12,7,  [(25,2)],               "payment_rejected",4,"SICEPAT",1,40000,    0,"2025-11-21 12:00","2025-11-22 08:00"),
    (12,8,  [(26,1),(25,1)],        "completed",       2,"JNE",   2,55000,    0,"2025-11-23 08:00","2025-12-03 10:00"),
    (12,9,  [(28,1)],               "processing",      0,"TIKI",  3,70000,    0,"2025-11-25 09:00","2025-11-26 09:00"),
    (12,3,  [(29,1)],               "shipped",         5,"JNE",   0,55000,    0,"2025-11-27 10:00","2025-11-29 09:00"),
    (12,5,  [(27,1)],               "completed",       3,"SICEPAT",1,60000,150000,"2025-11-29 11:00","2025-12-08 11:00"),
    (12,6,  [(26,1),(25,1)],        "cancelled",       1,"JNE",   2,55000,    0,"2025-12-01 12:00","2025-12-02 09:00"),
    (12,7,  [(28,1)],               "completed",       4,"TIKI",  3,70000,    0,"2025-12-03 08:00","2025-12-14 10:00"),
    (12,8,  [(25,1),(29,1)],        "received",        2,"JNE",   0,55000,    0,"2025-12-05 09:00","2025-12-12 11:00"),
    (12,9,  [(26,1)],               "pending",         0,"SICEPAT",1,45000,    0,"2025-12-07 10:00","2025-12-07 10:00"),
    (12,3,  [(27,1)],               "completed",       5,"JNE",   2,60000,100000,"2025-12-09 11:00","2025-12-20 12:00"),
    (12,5,  [(25,1),(28,1)],        "completed",       3,"TIKI",  3,75000,    0,"2025-12-11 12:00","2025-12-22 10:00"),
]

# Harga produk lookup
PROD_PRICE = {}
for sid, items in SELLER_PRODUCTS.items():
    for pid, price in items:
        PROD_PRICE[pid] = price

rej_i = can_i = 0

def build_transactions_sql():
    global rej_i, can_i
    trx_rows, det_rows = [], []
    det_id = 1

    for tid, (seller_id, buyer_id, items, status, pay_idx,
              courier, addr_idx, ship_cost, discount, created, updated) in enumerate(TRX, 1):

        subtotal   = sum(PROD_PRICE[p] * q for p, q in items)
        admin_fee  = 2000
        svc_fee    = 2000
        total      = subtotal + ship_cost + admin_fee + svc_fee - discount
        seller_amt = subtotal - svc_fee - admin_fee

        pay_name, pay_code = PAYMENT_METHODS[pay_idx]
        addr = ADDRESSES[addr_idx]

        has_proof    = status not in ("waiting_payment","cancelled")
        proof_val    = f"'proofs/proof_{tid:03d}.jpg'" if has_proof else "NULL"
        has_ship     = status in ("shipped","received","completed")
        ship_proof   = f"'ships/ship_{tid:03d}.jpg'"  if has_ship  else "NULL"
        tracking_val = f"'{courier}{tid:010d}'"        if has_ship  else "NULL"

        notes_val = "NULL"
        if status == "payment_rejected":
            n = SELLER_NOTES["payment_rejected"][rej_i % 5]; rej_i += 1
            notes_val = f"'{n}'"
        elif status == "cancelled":
            n = SELLER_NOTES["cancelled"][can_i % 5]; can_i += 1
            notes_val = f"'{n}'"

        trx_rows.append(
            f"({tid},{buyer_id},{seller_id},'{addr}','{pay_name}','{pay_code}',"
            f"{total:.2f},{discount},{admin_fee:.2f},{svc_fee:.2f},{seller_amt:.2f},"
            f"'{status}','{courier}','courier',{ship_cost:.2f},0.00,"
            f"{proof_val},{ship_proof},{tracking_val},{notes_val},"
            f"'{created}:00','{updated}:00')"
        )

        for pid, qty in items:
            price = PROD_PRICE[pid]
            det_rows.append(
                f"({det_id},{tid},{pid},{qty},{price:.2f},"
                f"'{created}:00','{created}:00')"
            )
            det_id += 1

    sql = (
        "SET FOREIGN_KEY_CHECKS=0;\n"
        "DELETE FROM transaction_details;\n"
        "DELETE FROM transactions;\n"
        "ALTER TABLE transactions AUTO_INCREMENT=1;\n"
        "ALTER TABLE transaction_details AUTO_INCREMENT=1;\n"
        "SET FOREIGN_KEY_CHECKS=1;\n"
        "INSERT INTO transactions"
        "(id,buyer_id,seller_id,shipping_address,payment_method,payment_method_code,"
        "total_amount,discount_total,admin_fee,service_fee,seller_amount,"
        "status,courier,delivery_type,shipping_cost,shipping_discount,"
        "payment_proof,shipping_proof,tracking_number,seller_notes,"
        "created_at,updated_at) VALUES\n"
        + ",\n".join(trx_rows) + ";\n"
        "INSERT INTO transaction_details"
        "(id,transaction_id,product_id,quantity,price,created_at,updated_at) VALUES\n"
        + ",\n".join(det_rows) + ";"
    )
    return sql

if __name__ == "__main__":
    print("1/4 Menambahkan users baru (id 5-12)...")
    run(NEW_USERS_SQL)

    print("2/4 Menambahkan produk baru (id 12-29)...")
    run(NEW_PRODUCTS_SQL)

    print("3/4 Membuat 120 transaksi...")
    sql = build_transactions_sql()
    run(sql)

    print("4/4 Verifikasi hasil...")
    out = run(
        "SELECT COUNT(*) AS total_users FROM users;"
        "SELECT COUNT(*) AS total_products FROM products;"
        "SELECT COUNT(*) AS total_trx FROM transactions;"
        "SELECT COUNT(*) AS total_items FROM transaction_details;"
        "SELECT status, COUNT(*) AS jumlah FROM transactions GROUP BY status ORDER BY jumlah DESC;"
        "SELECT u.name AS buyer, COUNT(*) AS transaksi FROM transactions t "
        "JOIN users u ON u.id=t.buyer_id GROUP BY t.buyer_id ORDER BY transaksi DESC;"
        "SELECT u.name AS seller, u.shop_name, COUNT(*) AS transaksi FROM transactions t "
        "JOIN users u ON u.id=t.seller_id GROUP BY t.seller_id ORDER BY transaksi DESC;"
    )
    print(out)
    print("Selesai! Semua data berhasil dimasukkan.")
