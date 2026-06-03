#!/usr/bin/env python3
"""
Insert 100 dummy transactions ke db_marketplace (MAMP MySQL)
Jalankan: python3 seeds/insert_dummy.py
"""

import subprocess
import sys

MYSQL = "/Applications/MAMP/Library/bin/mysql80/bin/mysql"
ARGS  = ["-u", "root", "-proot", "-P", "8889",
         "-S", "/Applications/MAMP/tmp/mysql/mysql.sock", "db_marketplace"]

def run_sql(sql: str):
    result = subprocess.run(
        [MYSQL] + ARGS,
        input=sql, capture_output=True, text=True
    )
    if result.returncode != 0:
        err = result.stderr.replace("mysql: [Warning] Using a password on the command line interface can be insecure.\n", "")
        if err.strip():
            print("ERROR:", err.strip())
            sys.exit(1)
    return result.stdout

# ── Data referensi ─────────────────────────────────────────────────────────────

# Products yang ada (id, nama, harga)
PRODUCTS = {
    5:  ("Kemeja Lengan Pendek",  85_000),
    6:  ("Dress Wanita",         185_000),
    8:  ("Kursi Plastik",         25_000),
    9:  ("Kursi Kayu",            96_000),
    10: ("Tempat Tidur",         850_000),
    11: ("Lemari Kayu",          500_000),
    1:  ("Samsung A24",       15_000_000),
    2:  ("Oppo A60",           7_000_000),
    3:  ("Oppo A78",           5_700_000),
    4:  ("iPhone 17 Pro Max", 22_000_000),
    7:  ("Scooters",           3_400_000),
}

PAYMENT_METHODS = [
    ("BCA Virtual Account",      "bank_bca"),
    ("Mandiri Virtual Account",  "bank_mandiri"),
    ("BNI Virtual Account",      "bank_bni"),
    ("GoPay",                    "gopay"),
    ("DANA",                     "dana"),
    ("OVO",                      "ovo"),
]

ADDRESSES = [
    "Jl. Sudirman No. 10, Makassar, Sulawesi Selatan 90111",
    "Jl. Pettarani No. 5, Makassar, Sulawesi Selatan 90222",
    "Jl. AP. Pettarani Blok A No. 3, Gowa, Sulawesi Selatan 92116",
    "Jl. Perintis Kemerdekaan No. 99, Makassar, Sulawesi Selatan 90245",
    "Jl. Urip Sumoharjo No. 17, Makassar, Sulawesi Selatan 90232",
]

COURIERS = ["JNE", "SICEPAT", "JNT", "TIKI", "GRAB"]

STATUSES = [
    "waiting_payment", "pending", "processing",
    "shipped", "received", "completed", "cancelled", "payment_rejected",
]

SELLER_NOTES = {
    "payment_rejected": [
        "Bukti pembayaran tidak jelas, mohon upload ulang",
        "Nominal transfer tidak sesuai",
        "Transfer dari rekening berbeda",
        "Bukti pembayaran expired",
        "Bukti tidak terbaca dengan jelas",
    ],
    "cancelled": [
        "Stok habis",
        "Pembeli membatalkan pesanan",
        "Produk tidak tersedia",
        "Duplikat pesanan",
        "Penjual tidak bisa memenuhi pesanan",
    ],
}

# ── 100 template transaksi ──────────────────────────────────────────────────────
# (items[(product_id, qty)], status, payment_idx, courier, addr_idx,
#  shipping_cost, discount, created_at, updated_at)

TRANSACTIONS = [
    # 1-10
    ([(5,1),(8,2)],         "completed",        0,"JNE",    0, 15000,     0, "2026-01-05 08:12","2026-01-10 14:30"),
    ([(6,1),(9,1)],         "completed",        1,"SICEPAT",1, 18000, 20000, "2026-01-06 09:45","2026-01-11 16:00"),
    ([(5,1)],               "shipped",          3,"GRAB",   2, 10000,     0, "2026-01-07 10:00","2026-01-09 11:20"),
    ([(6,2),(9,1)],         "processing",       5,"JNE",    3, 25000, 50000, "2026-01-08 11:30","2026-01-09 08:00"),
    ([(8,3),(5,1)],         "pending",          2,"POS",    0, 12000,     0, "2026-01-09 13:00","2026-01-09 13:00"),
    ([(6,1),(8,1)],         "waiting_payment",  0,"TIKI",   1, 20000,     0, "2026-01-10 14:15","2026-01-10 14:15"),
    ([(9,2),(5,1)],         "completed",        4,"JNT",    2, 22000, 30000, "2026-01-11 07:30","2026-01-17 10:00"),
    ([(5,1),(8,1)],         "cancelled",        3,"GRAB",   3, 15000,     0, "2026-01-12 09:00","2026-01-13 10:00"),
    ([(6,1),(9,2),(8,3)],   "completed",        0,"JNE",    0, 30000,     0, "2026-01-13 10:45","2026-01-20 12:00"),
    ([(5,1)],               "received",         1,"GRAB",   1,  8000,  5000, "2026-01-14 11:00","2026-01-16 09:30"),
    # 11-20
    ([(9,1),(8,2)],         "payment_rejected", 2,"SICEPAT",2, 18000,     0, "2026-01-15 12:30","2026-01-16 08:00"),
    ([(6,2),(9,1),(5,2)],   "completed",        0,"JNE",    3, 35000,100000, "2026-01-16 08:00","2026-01-24 15:00"),
    ([(5,1),(8,1)],         "processing",       5,"JNT",    0, 14000,     0, "2026-01-17 09:15","2026-01-18 10:00"),
    ([(9,1),(5,2)],         "shipped",          4,"TIKI",   1, 22000, 25000, "2026-01-18 10:30","2026-01-20 11:00"),
    ([(8,2)],               "completed",        3,"GRAB",   2,  8000,     0, "2026-01-19 13:45","2026-01-23 14:00"),
    ([(6,1),(9,2)],         "pending",          0,"JNE",    3, 28000, 50000, "2026-01-20 14:00","2026-01-20 14:00"),
    ([(8,3),(5,1)],         "completed",        1,"SICEPAT",0, 16000,     0, "2026-01-21 07:00","2026-01-28 09:00"),
    ([(5,2),(6,1)],         "processing",       0,"JNT",    1, 20000, 20000, "2026-01-22 08:30","2026-01-23 09:00"),
    ([(8,2)],               "waiting_payment",  5,"POS",    2, 12000,     0, "2026-01-23 09:45","2026-01-23 09:45"),
    ([(6,1),(9,1),(8,2)],   "completed",        4,"JNE",    3, 25000, 45000, "2026-01-24 11:00","2026-02-01 10:00"),
    # 21-30
    ([(5,1)],               "cancelled",        3,"GRAB",   0, 10000,     0, "2026-01-25 12:15","2026-01-26 08:00"),
    ([(9,1),(8,3)],         "shipped",          2,"TIKI",   1, 19000,     0, "2026-01-26 13:30","2026-01-28 10:30"),
    ([(6,2),(5,2)],         "received",         0,"JNE",    2, 28000, 60000, "2026-01-27 08:00","2026-02-02 11:00"),
    ([(8,2),(5,1)],         "completed",        1,"SICEPAT",3, 14000, 10000, "2026-01-28 09:00","2026-02-04 12:00"),
    ([(6,1),(9,1)],         "processing",       5,"JNT",    0, 24000,     0, "2026-01-29 10:15","2026-01-30 09:00"),
    ([(8,3)],               "pending",          3,"GRAB",   1,  9000,     0, "2026-01-30 11:30","2026-01-30 11:30"),
    ([(9,1),(5,2)],         "completed",        0,"JNE",    2, 20000, 15000, "2026-01-31 12:45","2026-02-07 14:00"),
    ([(6,1),(8,1)],         "waiting_payment",  4,"TIKI",   3, 17000,     0, "2026-02-01 08:00","2026-02-01 08:00"),
    ([(6,2),(9,2)],         "completed",        0,"JNE",    0, 32000, 80000, "2026-02-02 09:00","2026-02-10 13:00"),
    ([(5,1),(8,1)],         "shipped",          1,"SICEPAT",1, 13000,     0, "2026-02-03 10:00","2026-02-05 11:30"),
    # 31-40
    ([(9,2),(8,4)],         "received",         2,"JNT",    2, 23000, 40000, "2026-02-04 11:00","2026-02-09 09:00"),
    ([(5,1)],               "payment_rejected", 3,"GRAB",   3, 10000,     0, "2026-02-05 12:00","2026-02-06 07:30"),
    ([(6,1),(9,1)],         "completed",        5,"JNE",    0, 27000,     0, "2026-02-06 08:30","2026-02-13 15:00"),
    ([(8,2),(5,1)],         "processing",       0,"TIKI",   1, 15000,     0, "2026-02-07 09:30","2026-02-08 10:00"),
    ([(9,1),(6,1)],         "pending",          1,"SICEPAT",2, 20000,     0, "2026-02-08 10:30","2026-02-08 10:30"),
    ([(8,2)],               "completed",        3,"GRAB",   3,  7000,     0, "2026-02-09 11:30","2026-02-14 10:00"),
    ([(6,2),(9,2)],         "completed",        0,"JNE",    0, 38000,100000, "2026-02-10 12:45","2026-02-19 11:00"),
    ([(5,2),(8,2)],         "cancelled",        4,"JNT",    1, 18000,     0, "2026-02-11 08:00","2026-02-13 09:00"),
    ([(9,1),(6,1)],         "shipped",          2,"JNE",    2, 24000, 30000, "2026-02-12 09:00","2026-02-14 12:00"),
    ([(5,1),(8,1)],         "received",         1,"TIKI",   3, 12000,     0, "2026-02-13 10:00","2026-02-17 14:00"),
    # 41-50
    ([(6,1),(9,2)],         "completed",        5,"JNE",    0, 30000, 70000, "2026-02-14 11:00","2026-02-22 09:00"),
    ([(8,1),(5,1)],         "waiting_payment",  3,"SICEPAT",1, 16000,     0, "2026-02-15 12:00","2026-02-15 12:00"),
    ([(9,1),(5,2)],         "processing",       0,"JNT",    2, 22000, 20000, "2026-02-16 08:30","2026-02-17 09:00"),
    ([(8,3)],               "completed",        4,"GRAB",   3,  8000,     0, "2026-02-17 09:30","2026-02-23 11:00"),
    ([(6,2),(9,1)],         "completed",        0,"JNE",    0, 34000,     0, "2026-02-18 10:30","2026-02-26 12:00"),
    ([(5,1),(8,2)],         "pending",          2,"TIKI",   1, 14000,     0, "2026-02-19 11:30","2026-02-19 11:30"),
    ([(9,2),(6,1)],         "shipped",          1,"JNE",    2, 26000, 60000, "2026-02-20 12:30","2026-02-22 13:00"),
    ([(8,4),(5,1)],         "received",         5,"SICEPAT",3, 18000,     0, "2026-02-21 08:00","2026-02-25 10:00"),
    ([(5,1),(8,1)],         "completed",        3,"GRAB",   0, 12000,     0, "2026-02-22 09:00","2026-02-28 14:00"),
    ([(9,1),(6,1)],         "payment_rejected", 0,"JNT",    1, 24000, 25000, "2026-02-23 10:00","2026-02-24 08:30"),
    # 51-60
    ([(6,2),(9,1)],         "completed",        4,"JNE",    2, 33000,     0, "2026-02-24 11:00","2026-03-05 09:00"),
    ([(8,2),(5,1)],         "processing",       2,"TIKI",   3, 15000,     0, "2026-02-25 12:00","2026-02-26 09:30"),
    ([(9,1),(8,2)],         "cancelled",        1,"SICEPAT",0, 20000,     0, "2026-02-26 08:30","2026-02-27 10:00"),
    ([(8,2)],               "waiting_payment",  3,"GRAB",   1, 10000,     0, "2026-02-27 09:30","2026-02-27 09:30"),
    ([(6,1),(9,2)],         "completed",        0,"JNE",    2, 28000,     0, "2026-02-28 10:30","2026-03-08 11:00"),
    ([(5,2),(8,1)],         "shipped",          5,"JNT",    3, 19000,     0, "2026-03-01 11:30","2026-03-03 14:00"),
    ([(9,1),(5,2)],         "received",         4,"TIKI",   0, 23000, 35000, "2026-03-02 12:30","2026-03-07 09:00"),
    ([(8,4)],               "pending",          0,"SICEPAT",1, 12000,     0, "2026-03-03 08:00","2026-03-03 08:00"),
    ([(6,2),(9,2)],         "completed",        1,"JNE",    2, 36000,100000, "2026-03-04 09:00","2026-03-14 12:00"),
    ([(5,1),(8,2)],         "processing",       2,"GRAB",   3, 17000,     0, "2026-03-05 10:00","2026-03-06 09:30"),
    # 61-70
    ([(9,1),(6,1)],         "completed",        3,"JNE",    0, 25000,     0, "2026-03-06 11:00","2026-03-14 15:00"),
    ([(8,1)],               "waiting_payment",  5,"GRAB",   1,  8000,     0, "2026-03-07 12:00","2026-03-07 12:00"),
    ([(5,2),(8,1)],         "cancelled",        0,"JNT",    2, 21000,     0, "2026-03-08 08:30","2026-03-09 10:00"),
    ([(6,2),(9,1)],         "shipped",          4,"JNE",    3, 31000, 80000, "2026-03-09 09:30","2026-03-11 12:00"),
    ([(8,3),(5,1)],         "received",         1,"TIKI",   0, 14000,     0, "2026-03-10 10:30","2026-03-15 11:00"),
    ([(9,1),(8,2),(5,2)],   "completed",        0,"SICEPAT",1, 26000, 50000, "2026-03-11 11:30","2026-03-19 10:00"),
    ([(8,2),(5,1)],         "payment_rejected", 2,"JNE",    2, 16000,     0, "2026-03-12 12:30","2026-03-13 08:00"),
    ([(6,2),(9,2)],         "processing",       5,"JNT",    3, 34000,     0, "2026-03-13 08:00","2026-03-14 09:00"),
    ([(5,2),(8,2)],         "completed",        3,"GRAB",   0, 20000,     0, "2026-03-14 09:00","2026-03-21 13:00"),
    ([(9,1),(6,1)],         "pending",          0,"JNE",    1, 23000, 45000, "2026-03-15 10:00","2026-03-15 10:00"),
    # 71-80
    ([(8,4)],               "waiting_payment",  4,"TIKI",   2, 11000,     0, "2026-03-16 11:00","2026-03-16 11:00"),
    ([(6,1),(9,2),(8,3)],   "completed",        1,"JNE",    3, 30000, 70000, "2026-03-17 12:00","2026-03-26 11:00"),
    ([(5,2),(8,1)],         "shipped",          2,"SICEPAT",0, 18000,     0, "2026-03-18 08:30","2026-03-20 10:30"),
    ([(9,2),(6,1)],         "received",         5,"JNT",    1, 27000, 60000, "2026-03-19 09:30","2026-03-24 12:00"),
    ([(8,1)],               "cancelled",        3,"GRAB",   2,  9000,     0, "2026-03-20 10:30","2026-03-21 09:00"),
    ([(6,2),(9,1),(8,1)],   "completed",        0,"JNE",    3, 31000,     0, "2026-03-21 11:30","2026-03-30 14:00"),
    ([(5,1),(8,3)],         "processing",       4,"TIKI",   0, 16000,     0, "2026-03-22 12:30","2026-03-23 09:30"),
    ([(9,1),(5,2),(8,1)],   "completed",        1,"JNE",    1, 22000, 40000, "2026-03-23 08:00","2026-04-01 11:00"),
    ([(8,1),(5,1)],         "pending",          2,"SICEPAT",2, 14000,     0, "2026-03-24 09:00","2026-03-24 09:00"),
    ([(6,2),(9,1),(8,2)],   "shipped",          0,"JNE",    3, 36000,     0, "2026-03-25 10:00","2026-03-27 13:00"),
    # 81-90
    ([(5,2),(8,1)],         "received",         5,"JNT",    0, 19000,     0, "2026-03-26 11:00","2026-03-31 10:00"),
    ([(9,2),(6,1),(8,2)],   "completed",        3,"JNE",    1, 28000,     0, "2026-03-27 12:00","2026-04-05 12:00"),
    ([(8,2),(5,1)],         "waiting_payment",  4,"TIKI",   2, 15000,     0, "2026-03-28 08:30","2026-03-28 08:30"),
    ([(6,1),(9,1)],         "payment_rejected", 0,"SICEPAT",3, 24000,     0, "2026-03-29 09:30","2026-03-30 08:00"),
    ([(6,2),(9,1),(8,3)],   "processing",       1,"JNE",    0, 33000,     0, "2026-03-30 10:30","2026-03-31 09:00"),
    ([(5,1),(8,3)],         "completed",        2,"JNT",    1, 17000,     0, "2026-03-31 11:30","2026-04-08 11:00"),
    ([(9,1),(6,1),(8,2)],   "shipped",          5,"JNE",    2, 26000, 55000, "2026-04-01 12:30","2026-04-03 14:00"),
    ([(5,1)],               "cancelled",        3,"GRAB",   3, 10000,     0, "2026-04-02 08:00","2026-04-03 10:00"),
    ([(6,2),(9,1)],         "received",         0,"JNE",    0, 30000,     0, "2026-04-03 09:00","2026-04-08 11:00"),
    ([(8,2),(5,1)],         "completed",        4,"TIKI",   1, 15000,     0, "2026-04-04 10:00","2026-04-12 13:00"),
    # 91-100
    ([(5,2),(8,2),(6,1)],   "pending",          1,"SICEPAT",2, 22000,     0, "2026-04-05 11:00","2026-04-05 11:00"),
    ([(6,2),(9,2),(8,1)],   "completed",        0,"JNE",    3, 37000,     0, "2026-04-06 12:00","2026-04-16 10:00"),
    ([(8,5)],               "waiting_payment",  2,"JNT",    0, 13000,     0, "2026-04-07 08:30","2026-04-07 08:30"),
    ([(9,1),(6,1),(8,2)],   "processing",       5,"JNE",    1, 27000, 50000, "2026-04-08 09:30","2026-04-09 10:00"),
    ([(5,1),(8,2)],         "shipped",          3,"GRAB",   2, 18000,     0, "2026-04-09 10:30","2026-04-11 12:00"),
    ([(6,1),(9,2)],         "received",         0,"JNE",    3, 29000,     0, "2026-04-10 11:30","2026-04-15 14:00"),
    ([(8,2),(5,1)],         "completed",        4,"SICEPAT",0, 12000,     0, "2026-04-11 12:30","2026-04-19 11:00"),
    ([(6,2),(9,1),(8,2)],   "cancelled",        1,"JNE",    1, 34000,     0, "2026-04-12 08:00","2026-04-13 09:00"),
    ([(5,2),(8,2)],         "completed",        0,"TIKI",   2, 20000,     0, "2026-04-13 09:00","2026-04-21 12:00"),
    ([(9,1),(6,1),(8,3)],   "processing",       5,"JNT",    3, 26000,     0, "2026-04-14 10:00","2026-04-15 09:30"),
]

REJECTED_NOTE_IDX = 0
CANCELLED_NOTE_IDX = 0

def build_sql():
    lines = ["SET FOREIGN_KEY_CHECKS=0;",
             "DELETE FROM transaction_details;",
             "DELETE FROM transactions;",
             "ALTER TABLE transactions AUTO_INCREMENT = 1;",
             "ALTER TABLE transaction_details AUTO_INCREMENT = 1;",
             "SET FOREIGN_KEY_CHECKS=1;"]

    global REJECTED_NOTE_IDX, CANCELLED_NOTE_IDX
    detail_id = 1
    trx_rows = []
    detail_rows = []

    for trx_id, (items, status, pay_idx, courier, addr_idx,
                 shipping_cost, discount, created, updated) in enumerate(TRANSACTIONS, start=1):

        subtotal = sum(PRODUCTS[pid][1] * qty for pid, qty in items)
        admin_fee = 2000
        service_fee = 2000
        total = subtotal + shipping_cost + admin_fee + service_fee - discount
        seller_amount = subtotal - service_fee - admin_fee

        pay_name, pay_code = PAYMENT_METHODS[pay_idx]
        address_txt = ADDRESSES[addr_idx]

        has_proof = status not in ("waiting_payment", "cancelled")
        payment_proof = f"proofs/proof_{trx_id:03d}.jpg" if has_proof else "NULL"
        has_shipping = status in ("shipped", "received", "completed")
        shipping_proof = f"ships/ship_{trx_id:03d}.jpg" if has_shipping else "NULL"
        has_tracking = status in ("shipped", "received", "completed")
        tracking = f"{courier}{trx_id:010d}" if has_tracking else "NULL"

        notes_val = "NULL"
        if status == "payment_rejected":
            note = SELLER_NOTES["payment_rejected"][REJECTED_NOTE_IDX % len(SELLER_NOTES["payment_rejected"])]
            notes_val = f"'{note}'"
            REJECTED_NOTE_IDX += 1
        elif status == "cancelled":
            note = SELLER_NOTES["cancelled"][CANCELLED_NOTE_IDX % len(SELLER_NOTES["cancelled"])]
            notes_val = f"'{note}'"
            CANCELLED_NOTE_IDX += 1

        proof_val = f"'{payment_proof}'" if payment_proof != "NULL" else "NULL"
        shipping_proof_val = f"'{shipping_proof}'" if shipping_proof != "NULL" else "NULL"
        tracking_val = f"'{tracking}'" if tracking != "NULL" else "NULL"

        trx_rows.append(
            f"({trx_id}, 3, 2, '{address_txt}', '{pay_name}', '{pay_code}', "
            f"{total:.2f}, {discount}, {admin_fee:.2f}, {service_fee:.2f}, "
            f"{seller_amount:.2f}, '{status}', '{courier}', 'courier', "
            f"{shipping_cost:.2f}, 0.00, "
            f"{proof_val}, {shipping_proof_val}, {tracking_val}, "
            f"{notes_val}, "
            f"'{created}:00', '{updated}:00')"
        )

        for pid, qty in items:
            price = PRODUCTS[pid][1]
            detail_rows.append(
                f"({detail_id}, {trx_id}, {pid}, {qty}, {price:.2f}, "
                f"'{created}:00', '{created}:00')"
            )
            detail_id += 1

    lines.append(
        "INSERT INTO transactions "
        "(id, buyer_id, seller_id, shipping_address, payment_method, payment_method_code, "
        "total_amount, discount_total, admin_fee, service_fee, seller_amount, "
        "status, courier, delivery_type, shipping_cost, shipping_discount, "
        "payment_proof, shipping_proof, tracking_number, seller_notes, "
        "created_at, updated_at) VALUES\n" +
        ",\n".join(trx_rows) + ";"
    )

    lines.append(
        "INSERT INTO transaction_details "
        "(id, transaction_id, product_id, quantity, price, created_at, updated_at) VALUES\n" +
        ",\n".join(detail_rows) + ";"
    )

    return "\n".join(lines)


if __name__ == "__main__":
    print("Membuat SQL...")
    sql = build_sql()

    print("Memasukkan data ke db_marketplace...")
    run_sql(sql)
    print("Selesai!")

    out = run_sql(
        "SELECT COUNT(*) AS total_trx FROM transactions; "
        "SELECT COUNT(*) AS total_items FROM transaction_details; "
        "SELECT status, COUNT(*) AS jumlah FROM transactions GROUP BY status ORDER BY jumlah DESC;"
    )
    print(out)
