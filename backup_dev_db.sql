--
-- PostgreSQL database dump
--

\restrict dLPpdMXQxpcBMbPiusQ2GyBEkRFL9iXDC5awXBKPCovR88UGlbZAxsGSstiQiH8

-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: alat_olahraga; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.alat_olahraga (
    reservation_end timestamp(6) without time zone,
    reservation_start timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    id uuid NOT NULL,
    CONSTRAINT alat_olahraga_status_check CHECK (((status)::text = ANY ((ARRAY['TERSEDIA'::character varying, 'DISEWAKAN'::character varying, 'DALAM_PERBAIKAN'::character varying])::text[])))
);


ALTER TABLE public.alat_olahraga OWNER TO "gor-gemilangcondet-dev";

--
-- Name: barang; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.barang (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    name character varying(255) NOT NULL,
    price double precision NOT NULL,
    stock bigint NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone
);


ALTER TABLE public.barang OWNER TO "gor-gemilangcondet-dev";

--
-- Name: bookings; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.bookings (
    id uuid NOT NULL,
    booking_date date NOT NULL,
    created_at timestamp(6) without time zone,
    customer_name character varying(100) NOT NULL,
    customer_phone character varying(20),
    start_time time(0) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone,
    lapangan_id uuid NOT NULL,
    CONSTRAINT bookings_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.bookings OWNER TO "gor-gemilangcondet-dev";

--
-- Name: courts; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.courts (
    id integer NOT NULL,
    name character varying(50) NOT NULL
);


ALTER TABLE public.courts OWNER TO "gor-gemilangcondet-dev";

--
-- Name: lapangan; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.lapangan (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    image_url character varying(255),
    jenis_lantai character varying(255),
    kode character varying(255) NOT NULL,
    maintenance_end timestamp(6) without time zone,
    maintenance_start timestamp(6) without time zone,
    name character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    tarif_per_jam double precision NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT lapangan_status_check CHECK (((status)::text = ANY ((ARRAY['TERSEDIA'::character varying, 'DISEWAKAN'::character varying, 'DALAM_PERBAIKAN'::character varying])::text[]))),
    CONSTRAINT lapangan_type_check CHECK (((type)::text = 'BADMINTON'::text))
);


ALTER TABLE public.lapangan OWNER TO "gor-gemilangcondet-dev";

--
-- Name: lapangan_fasilitas; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.lapangan_fasilitas (
    lapangan_id uuid NOT NULL,
    fasilitas character varying(255)
);


ALTER TABLE public.lapangan_fasilitas OWNER TO "gor-gemilangcondet-dev";

--
-- Name: lapangan_log; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.lapangan_log (
    id uuid NOT NULL,
    change_description text NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    lapangan_id uuid NOT NULL,
    user_id uuid NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.lapangan_log OWNER TO "gor-gemilangcondet-dev";

--
-- Name: pembayaran; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.pembayaran (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    method character varying(255) NOT NULL,
    payment_date timestamp(6) without time zone,
    price double precision NOT NULL,
    receipt character varying(255),
    reservation_id uuid NOT NULL,
    staff_id uuid,
    status character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id uuid NOT NULL,
    CONSTRAINT pembayaran_method_check CHECK (((method)::text = ANY ((ARRAY['CASH'::character varying, 'TRANSFER'::character varying, 'QRIS'::character varying])::text[]))),
    CONSTRAINT pembayaran_status_check CHECK (((status)::text = ANY ((ARRAY['BELUM_DIBAYAR'::character varying, 'LUNAS'::character varying, 'DP'::character varying, 'DIBATALKAN'::character varying, 'REFUND'::character varying])::text[]))),
    CONSTRAINT pembayaran_type_check CHECK (((type)::text = ANY ((ARRAY['FULL_PAYMENT_RESERVASI'::character varying, 'DOWN_PAYMENT_RESERVASI'::character varying, 'PELUNASAN_RESERVASI'::character varying, 'RENTAL'::character varying, 'PEMBELIAN_ALAT_OLAHRAGA'::character varying])::text[])))
);


ALTER TABLE public.pembayaran OWNER TO "gor-gemilangcondet-dev";

--
-- Name: reservasi; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.reservasi (
    id uuid NOT NULL,
    batch_id uuid,
    created_at timestamp(6) without time zone,
    jumlah_orang integer NOT NULL,
    nama_wakil character varying(255) NOT NULL,
    nomor_telepon character varying(255) NOT NULL,
    payment_deadline timestamp(6) without time zone,
    payment_id uuid,
    payment_proof_url character varying(255),
    reservation_end timestamp(6) without time zone NOT NULL,
    reservation_start timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    total_payment double precision NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id uuid NOT NULL,
    lapangan_id uuid NOT NULL,
    CONSTRAINT reservasi_status_check CHECK (((status)::text = ANY ((ARRAY['BELUM_DIBAYAR'::character varying, 'MENUNGGU_KONFIRMASI_STAF'::character varying, 'DIKONFIRMASI'::character varying, 'DITOLAK'::character varying, 'EXPIRED'::character varying, 'DOWN_PAYMENT'::character varying, 'DIBAYAR'::character varying, 'DIBATALKAN'::character varying, 'SELESAI'::character varying])::text[])))
);


ALTER TABLE public.reservasi OWNER TO "gor-gemilangcondet-dev";

--
-- Name: reservasi_rent_list; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.reservasi_rent_list (
    reservasi_id uuid NOT NULL,
    barang_id uuid
);


ALTER TABLE public.reservasi_rent_list OWNER TO "gor-gemilangcondet-dev";

--
-- Name: roles; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.roles (
    id integer NOT NULL,
    role_name character varying(255) NOT NULL,
    CONSTRAINT roles_role_name_check CHECK (((role_name)::text = ANY ((ARRAY['GUEST'::character varying, 'MEMBER'::character varying, 'STAF_LAPANGAN'::character varying, 'STAF_TOKO'::character varying, 'OWNER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.roles OWNER TO "gor-gemilangcondet-dev";

--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE public.roles ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: transaksi; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.transaksi (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    diskon double precision NOT NULL,
    grand_total double precision NOT NULL,
    payment_method character varying(255) NOT NULL,
    staff_id uuid NOT NULL,
    status character varying(255) NOT NULL,
    subtotal double precision NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT transaksi_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'TRANSFER'::character varying, 'QRIS'::character varying])::text[]))),
    CONSTRAINT transaksi_status_check CHECK (((status)::text = ANY ((ARRAY['SELESAI'::character varying, 'DIBATALKAN'::character varying])::text[])))
);


ALTER TABLE public.transaksi OWNER TO "gor-gemilangcondet-dev";

--
-- Name: transaksi_detail; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.transaksi_detail (
    id uuid NOT NULL,
    harga_satuan double precision NOT NULL,
    kuantitas integer NOT NULL,
    subtotal double precision NOT NULL,
    barang_id uuid NOT NULL,
    transaksi_id uuid NOT NULL
);


ALTER TABLE public.transaksi_detail OWNER TO "gor-gemilangcondet-dev";

--
-- Name: user_status; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.user_status (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT user_status_name_check CHECK (((name)::text = ANY ((ARRAY['PENDING'::character varying, 'AKTIF'::character varying, 'NON_AKTIF'::character varying, 'SUSPENDED'::character varying, 'BANNED'::character varying])::text[])))
);


ALTER TABLE public.user_status OWNER TO "gor-gemilangcondet-dev";

--
-- Name: user_status_id_seq; Type: SEQUENCE; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE public.user_status ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.user_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: gor-gemilangcondet-dev
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    membership_end timestamp(6) without time zone,
    membership_start timestamp(6) without time zone,
    password character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    role_id integer NOT NULL,
    status_id integer NOT NULL,
    banned_at timestamp(6) without time zone
);


ALTER TABLE public.users OWNER TO "gor-gemilangcondet-dev";

--
-- Data for Name: alat_olahraga; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.alat_olahraga (reservation_end, reservation_start, status, id) FROM stdin;
\N	\N	TERSEDIA	0382eb10-3703-49b7-842a-889efe3f7640
\.


--
-- Data for Name: barang; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.barang (id, created_at, name, price, stock, type, updated_at) FROM stdin;
0382eb10-3703-49b7-842a-889efe3f7640	2026-04-03 15:17:20.942678	Raket Badminton Premium	25000	20	RAKET	2026-04-03 15:17:20.942678
5491174f-7fd5-4605-b004-2c923493a815	2026-04-17 22:10:32.615495	Nasi Goreng	15000	50	MAKANAN	2026-04-17 22:10:32.615495
ddf7ad2a-e6f3-487f-89f6-e56bd4fda550	2026-04-17 22:10:32.615495	Mie Goreng	12000	50	MAKANAN	2026-04-17 22:10:32.615495
1532845f-9435-48cb-aadc-3e30d87a1db0	2026-04-17 22:10:32.615495	Roti Bakar	10000	30	MAKANAN	2026-04-17 22:10:32.615495
53c80f87-ee15-4975-b5c8-bf2941e3eea6	2026-04-17 22:10:32.615495	Kentang Goreng	12000	40	MAKANAN	2026-04-17 22:10:32.615495
4356c97e-3ed3-443c-ab58-ca5c1e7c9135	2026-04-17 22:10:32.615495	Raket Yonex	450000	10	RAKET	2026-04-17 22:10:32.615495
12a32d6c-ebd7-4d08-abfc-ff5da37817ad	2026-04-17 22:10:32.615495	Raket Li-Ning	350000	8	RAKET	2026-04-17 22:10:32.615495
2fb23e0a-f289-46d4-83c5-aeae25293d97	2026-04-17 22:10:32.615495	Teh Botol	7000	100	MINUMAN	2026-04-17 22:10:32.615495
5a76290c-41f7-491a-9da5-8ad7a69395b2	2026-04-17 22:10:32.615495	Air Mineral	5000	100	MINUMAN	2026-04-17 22:10:32.615495
5359fc16-a760-4fae-b61d-556cdfb1243b	2026-04-17 22:10:32.615495	Kopi Hitam	10000	60	MINUMAN	2026-04-17 22:10:32.615495
a1f68f5a-422c-4a23-8638-b8266de0e96e	2026-04-17 22:10:32.615495	Es Jeruk	8000	60	MINUMAN	2026-04-17 22:10:32.615495
4ec23a49-acd5-4f99-af0c-2bdd81d067e2	2026-04-17 22:10:32.615495	Pocari Sweat	10000	80	MINUMAN	2026-04-17 22:10:32.615495
\.


--
-- Data for Name: bookings; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.bookings (id, booking_date, created_at, customer_name, customer_phone, start_time, status, updated_at, lapangan_id) FROM stdin;
\.


--
-- Data for Name: courts; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.courts (id, name) FROM stdin;
\.


--
-- Data for Name: lapangan; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.lapangan (id, created_at, image_url, jenis_lantai, kode, maintenance_end, maintenance_start, name, status, tarif_per_jam, type, updated_at) FROM stdin;
7c9b649d-9377-46fb-b65b-b035d5e45587	2026-04-03 15:17:20.885177	\N	Vinyl	BDM-002	\N	\N	Badminton 2	TERSEDIA	50000	BADMINTON	2026-04-03 15:17:20.885177
af50524a-7e89-442f-b180-fad21bb2a7a3	2026-04-03 15:17:20.885177	\N	Vinyl	BDM-003	\N	\N	Badminton 3	TERSEDIA	150000	BADMINTON	2026-04-03 15:17:20.885177
e0f0c016-f955-400c-ac3a-e0ff29818065	2026-04-12 17:04:17.795867	\N	Vinyl	BDM005	\N	\N	Badminton 5	TERSEDIA	100000	BADMINTON	2026-04-12 17:04:17.795867
aab66393-2f53-4172-9b27-b7677bf8f3ae	2026-04-03 15:17:20.885177	\N	Vinyl	BDM-001	\N	\N	Badminton 1	TERSEDIA	50000	BADMINTON	2026-04-12 17:56:37.793176
13bd4229-8b87-453a-b211-b593c2b13e16	2026-04-12 17:03:35.776766	court-13bd4229-8b87-453a-b211-b593c2b13e16-1775991065601.png	Semen	BDM 004	\N	\N	Badminton 4	TERSEDIA	100000	BADMINTON	2026-04-17 22:31:26.535516
\.


--
-- Data for Name: lapangan_fasilitas; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.lapangan_fasilitas (lapangan_id, fasilitas) FROM stdin;
7c9b649d-9377-46fb-b65b-b035d5e45587	LED Lighting
7c9b649d-9377-46fb-b65b-b035d5e45587	Fan
7c9b649d-9377-46fb-b65b-b035d5e45587	Vinyl Flooring
af50524a-7e89-442f-b180-fad21bb2a7a3	LED Lighting
af50524a-7e89-442f-b180-fad21bb2a7a3	Fan
af50524a-7e89-442f-b180-fad21bb2a7a3	Vinyl Flooring
aab66393-2f53-4172-9b27-b7677bf8f3ae	LED Lighting
aab66393-2f53-4172-9b27-b7677bf8f3ae	Fan
aab66393-2f53-4172-9b27-b7677bf8f3ae	Vinyl Flooring
\.


--
-- Data for Name: lapangan_log; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.lapangan_log (id, change_description, created_at, lapangan_id, user_id, username) FROM stdin;
b3b0c0d0-483f-4269-b377-c1722ffe1874	Nama: Badminton 3 -> Badminton 4 | Harga: Rp 50.000 -> Rp 100.000 | Fasilitas: + Fan	2026-04-12 17:37:00.199004	13bd4229-8b87-453a-b211-b593c2b13e16	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
b366de8a-67c5-418b-8488-ff0cbd60dd03	Fasilitas: 	2026-04-12 17:37:28.991894	13bd4229-8b87-453a-b211-b593c2b13e16	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
cc7029aa-447f-4593-9ed7-dd3b367d66b5	Fasilitas: 	2026-04-12 17:37:48.630824	13bd4229-8b87-453a-b211-b593c2b13e16	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
2c3c893e-1f62-4237-bf5c-88074bc8f6e6	Fasilitas: 	2026-04-12 17:51:07.717846	13bd4229-8b87-453a-b211-b593c2b13e16	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
7479385f-8aac-4781-a48f-3ef5c5f459e2	Fasilitas: + Scoreboard	2026-04-12 17:56:29.247292	aab66393-2f53-4172-9b27-b7677bf8f3ae	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
85c10164-8ca5-4e8e-8800-a883876cbc46	Fasilitas: - Scoreboard	2026-04-12 17:56:37.793176	aab66393-2f53-4172-9b27-b7677bf8f3ae	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
23551c5e-7f06-4e32-aca0-765c91e9aef9	Fasilitas: - Fan	2026-04-17 22:31:26.535516	13bd4229-8b87-453a-b211-b593c2b13e16	d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf_lapangan1
\.


--
-- Data for Name: pembayaran; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.pembayaran (id, created_at, method, payment_date, price, receipt, reservation_id, staff_id, status, type, updated_at, user_id) FROM stdin;
63c1b23d-f807-4382-8002-f17d38620a72	2026-03-09 11:37:23.089985	TRANSFER	2026-03-09 11:37:23.089985	50000	proof_ac6fd9e3-e452-414d-9f0f-f4e8f0d6a305_1773031023196.png	ac6fd9e3-e452-414d-9f0f-f4e8f0d6a305	4988be45-f2d4-4a32-8888-b4b56a787bdf	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-09 11:37:23.089985	52192372-04aa-424e-9062-cf929e91020f
bbcf6a81-a704-47c1-9336-cdeee5ed63b5	2026-03-09 18:31:34.589862	TRANSFER	2026-03-09 18:31:34.589862	50000	proof_8196c38e-f584-4295-acc7-385da3a94676_1773055878275.png	8196c38e-f584-4295-acc7-385da3a94676	4988be45-f2d4-4a32-8888-b4b56a787bdf	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-09 18:31:34.589862	52192372-04aa-424e-9062-cf929e91020f
912fcf96-dc3d-4324-b536-bc467a3f1358	2026-03-09 18:56:59.907924	TRANSFER	2026-03-09 18:56:59.907924	50000	proof_64b3f06f-7549-4ef9-8f4f-cfb9f78a36cc_1773057398753.png	64b3f06f-7549-4ef9-8f4f-cfb9f78a36cc	4988be45-f2d4-4a32-8888-b4b56a787bdf	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-09 18:56:59.907924	da633a33-0368-47c7-9ffb-07e6b0af2bda
b8e50920-f45d-4913-a25e-5b48a9206545	2026-03-09 21:29:29.266393	TRANSFER	2026-03-09 21:29:29.266393	50000	proof_7dcb0e44-6aab-47c9-a751-78ebb319720e_1773066560795.png	7dcb0e44-6aab-47c9-a751-78ebb319720e	4988be45-f2d4-4a32-8888-b4b56a787bdf	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-09 21:29:29.266393	da633a33-0368-47c7-9ffb-07e6b0af2bda
afb053f3-3998-45a0-b6a0-419bcbee113e	2026-03-09 21:30:35.210484	TRANSFER	2026-03-09 21:30:35.210484	50000	proof_d28d1bb0-7d74-4a80-a491-4666e39b15cc_1773066627651.png	d28d1bb0-7d74-4a80-a491-4666e39b15cc	4988be45-f2d4-4a32-8888-b4b56a787bdf	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-09 21:30:35.210484	da633a33-0368-47c7-9ffb-07e6b0af2bda
526a8cb6-d77a-490c-9aa0-6e50cd2abcb0	2026-03-10 19:17:33.806506	TRANSFER	2026-03-10 19:17:33.806506	50000	proof_cf092e28-7947-43f5-9dd6-608e349b967b_1773145010790.png	cf092e28-7947-43f5-9dd6-608e349b967b	d9b9fccb-a175-4d91-8b8f-2b296588bf44	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-10 19:17:33.806506	52192372-04aa-424e-9062-cf929e91020f
b3e59039-5684-4caf-ad89-d4a2a33f2ad6	2026-03-10 20:02:07.771803	TRANSFER	2026-03-10 20:02:07.771803	50000	proof_868a252b-de1d-4b0e-991f-323f8dbed643_1773147705003.png	868a252b-de1d-4b0e-991f-323f8dbed643	d9b9fccb-a175-4d91-8b8f-2b296588bf44	LUNAS	FULL_PAYMENT_RESERVASI	2026-03-10 20:02:07.771803	52192372-04aa-424e-9062-cf929e91020f
300a7454-140d-43ec-a8c1-c5735e8e3f82	2026-04-02 15:19:49.968624	TRANSFER	2026-04-02 15:19:49.968624	50000	proof_7a57376a-09b1-444e-a6dc-9c9a7873c063_1775117983105.png	7a57376a-09b1-444e-a6dc-9c9a7873c063	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-02 15:19:49.968624	da633a33-0368-47c7-9ffb-07e6b0af2bda
057a7441-0752-469a-a911-15dbbb9d5310	2026-04-02 15:41:03.528921	TRANSFER	2026-04-02 15:41:03.528921	50000	proof_87c82a65-0ed2-4893-a770-265547d9c35d_1775119250383.png	87c82a65-0ed2-4893-a770-265547d9c35d	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-02 15:41:03.528921	da633a33-0368-47c7-9ffb-07e6b0af2bda
cb366c41-368f-4aa1-99dc-809ae3f0ccfb	2026-04-02 16:27:14.84729	TRANSFER	2026-04-02 16:27:14.84729	50000	proof_c1827814-0cfe-4da4-8d68-0db456707734_1775122023354.png	c1827814-0cfe-4da4-8d68-0db456707734	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-02 16:27:14.84729	da633a33-0368-47c7-9ffb-07e6b0af2bda
b23035e5-86d3-4145-9809-602a5c2fe13c	2026-04-02 17:08:49.640043	TRANSFER	2026-04-02 17:08:49.640043	50000	proof_13a58f41-b42a-4003-868d-13c8b70589a0_1775124524347.png	13a58f41-b42a-4003-868d-13c8b70589a0	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-02 17:08:49.640043	da633a33-0368-47c7-9ffb-07e6b0af2bda
5a6fa142-3847-4d65-8bd9-6c2ac6908f4a	2026-04-03 14:30:06.020978	TRANSFER	2026-04-03 14:30:06.020978	100000	proof_e8b5dee4-5f8b-4ff2-9ef3-caa9e2615582_1775201384750.png	e8b5dee4-5f8b-4ff2-9ef3-caa9e2615582	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 14:30:06.020978	da633a33-0368-47c7-9ffb-07e6b0af2bda
c0afd95e-3b7d-4ffd-933e-8002f9989e6c	2026-04-03 14:44:52.056822	TRANSFER	2026-04-03 14:44:52.056822	115000	proof_28802534-10c7-4f62-ac9e-dc538cc7cb02_1775202283787.png	28802534-10c7-4f62-ac9e-dc538cc7cb02	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 14:44:52.056822	da633a33-0368-47c7-9ffb-07e6b0af2bda
7815fb1d-6f57-4301-bd22-982ed3235174	2026-04-03 15:51:05.041057	TRANSFER	2026-04-03 15:51:05.041057	100000	proof_4531f9e8-c251-4d88-bf61-0eca1127b8c2_1775204452329.png	4531f9e8-c251-4d88-bf61-0eca1127b8c2	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 15:51:05.041057	da633a33-0368-47c7-9ffb-07e6b0af2bda
b4b87ccc-abd2-43ea-bab1-c6407700987b	2026-04-03 16:19:52.364134	TRANSFER	2026-04-03 16:19:52.364134	50000	proof_4bb5f763-4f97-4c45-98cc-4e9efe80b126_1775207986991.png	4bb5f763-4f97-4c45-98cc-4e9efe80b126	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 16:19:52.364134	da633a33-0368-47c7-9ffb-07e6b0af2bda
9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	2026-04-03 17:24:49.663239	TRANSFER	2026-04-03 17:24:49.663239	100000	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	aad70704-9925-42ec-8e33-83da1b0c17bc	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 17:24:49.663239	da633a33-0368-47c7-9ffb-07e6b0af2bda
3859a493-8140-4b22-9647-31deade62444	2026-04-03 17:33:27.541011	TRANSFER	2026-04-03 17:33:27.541011	125000	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	ea1ef557-c67f-4307-811e-d1ccfb7c6d02	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 17:33:27.541011	da633a33-0368-47c7-9ffb-07e6b0af2bda
837b85ef-4c63-4a77-aab2-2b66f916cacb	2026-04-03 17:38:55.145171	TRANSFER	2026-04-03 17:38:55.145171	125000	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	a83238e6-30e8-4943-92b5-72a1498ab341	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 17:38:55.145171	da633a33-0368-47c7-9ffb-07e6b0af2bda
337129dc-e654-4267-ab07-01e7daf7e8b4	2026-04-03 17:52:41.27869	TRANSFER	2026-04-03 17:52:41.27869	125000	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	a947e1d0-5936-4b0a-900a-591fd3dcc1fa	6e93b343-7b7e-45c2-bafb-48680152b0e0	LUNAS	FULL_PAYMENT_RESERVASI	2026-04-03 17:52:41.27869	da633a33-0368-47c7-9ffb-07e6b0af2bda
\.


--
-- Data for Name: reservasi; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.reservasi (id, batch_id, created_at, jumlah_orang, nama_wakil, nomor_telepon, payment_deadline, payment_id, payment_proof_url, reservation_end, reservation_start, status, total_payment, updated_at, user_id, lapangan_id) FROM stdin;
4bb5f763-4f97-4c45-98cc-4e9efe80b126	\N	2026-04-03 16:19:39.972664	1	sher	08456345634	2026-04-03 16:29:39.972664	b4b87ccc-abd2-43ea-bab1-c6407700987b	proof_4bb5f763-4f97-4c45-98cc-4e9efe80b126_1775207986991.png	2026-04-05 12:00:00	2026-04-05 11:00:00	DIKONFIRMASI	50000	2026-04-03 16:19:52.364134	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
e7ce4c6d-0832-4512-b3fd-e9234df641d0	6733ec0d-4058-47c0-99ca-bbd2ef6c19a7	2026-04-03 15:20:45.468103	1	sher	085323423423	2026-04-03 15:30:45.468103	7815fb1d-6f57-4301-bd22-982ed3235174	proof_4531f9e8-c251-4d88-bf61-0eca1127b8c2_1775204452329.png	2026-04-06 14:00:00	2026-04-06 13:00:00	DIKONFIRMASI	50000	2026-04-03 17:16:34.187144	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
c8df6e51-2ce3-4524-a37d-941ba9811c38	6733ec0d-4058-47c0-99ca-bbd2ef6c19a7	2026-04-03 15:20:45.468103	1	sher	085323423423	2026-04-03 15:30:45.468103	7815fb1d-6f57-4301-bd22-982ed3235174	proof_4531f9e8-c251-4d88-bf61-0eca1127b8c2_1775204452329.png	2026-04-06 18:00:00	2026-04-06 17:00:00	DIKONFIRMASI	50000	2026-04-03 17:16:34.187144	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
ecda4199-416b-48f4-9021-4bbd762e0a0b	6733ec0d-4058-47c0-99ca-bbd2ef6c19a7	2026-04-03 15:20:45.468103	1	sher	085323423423	2026-04-03 15:30:45.468103	7815fb1d-6f57-4301-bd22-982ed3235174	proof_4531f9e8-c251-4d88-bf61-0eca1127b8c2_1775204452329.png	2026-04-06 20:00:00	2026-04-06 19:00:00	DIKONFIRMASI	50000	2026-04-03 17:16:34.187144	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
621ab94e-a78f-4b7b-9066-0f2bc233bb73	91b18983-5ccf-48e9-95cd-33fd3d5023cb	2026-04-03 17:24:03.966446	1	sher3	0853427373277	2026-04-03 17:34:03.966446	9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	2026-04-03 19:00:00	2026-04-03 18:00:00	DIKONFIRMASI	100000	2026-04-03 17:30:48.20207	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
89935dec-dfe2-4265-afcb-5abb494a1367	91b18983-5ccf-48e9-95cd-33fd3d5023cb	2026-04-03 17:24:03.966446	1	sher3	0853427373277	2026-04-03 17:34:03.966446	9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	2026-04-03 20:00:00	2026-04-03 19:00:00	DIKONFIRMASI	50000	2026-04-03 17:30:48.20207	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
3572e4c1-54f2-4bb1-a98e-d90399ba82aa	91b18983-5ccf-48e9-95cd-33fd3d5023cb	2026-04-03 17:24:03.966446	1	sher3	0853427373277	2026-04-03 17:34:03.966446	9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	2026-04-03 21:00:00	2026-04-03 20:00:00	DIKONFIRMASI	50000	2026-04-03 17:30:48.20207	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
55bd8e9a-6226-4bac-9d72-f1f7eb4ef6b0	91b18983-5ccf-48e9-95cd-33fd3d5023cb	2026-04-03 17:24:03.966446	1	sher3	0853427373277	2026-04-03 17:34:03.966446	9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	2026-04-03 22:00:00	2026-04-03 21:00:00	DIKONFIRMASI	50000	2026-04-03 17:30:48.20207	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
0c0bde1b-7e82-4e6d-99a7-87de91cf4dfe	91b18983-5ccf-48e9-95cd-33fd3d5023cb	2026-04-03 17:24:03.966446	1	sher3	0853427373277	2026-04-03 17:34:03.966446	9d4bf251-b9bf-45be-a7b9-a3951edfbf4a	proof_aad70704-9925-42ec-8e33-83da1b0c17bc_1775211866878.png	2026-04-03 23:00:00	2026-04-03 22:00:00	DIKONFIRMASI	50000	2026-04-03 17:30:48.20207	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
e4e9d893-9cb6-402b-a2d2-235bcd861617	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 08:00:00	2026-04-09 07:00:00	DIKONFIRMASI	75000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
a23c78ce-3bd9-4526-8b51-85e3e63690ec	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 10:00:00	2026-04-09 09:00:00	DIKONFIRMASI	50000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
aaa11732-f955-48c1-a5fd-b82b612062bb	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 14:00:00	2026-04-09 13:00:00	DIKONFIRMASI	50000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
2a78fe6c-67e5-4068-ab7d-2d2df583ac9a	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 16:00:00	2026-04-09 15:00:00	DIKONFIRMASI	50000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
5cd742cf-871d-4222-a0a1-d7371abeeb81	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 20:00:00	2026-04-09 19:00:00	DIKONFIRMASI	50000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
23950308-9d4f-4162-926a-ae257c896dac	bc1919d1-57fb-4c44-bfa7-20b7b2c97a2d	2026-04-03 17:32:41.956752	1	sher2	086324234324	2026-04-03 17:42:41.956752	3859a493-8140-4b22-9647-31deade62444	proof_ea1ef557-c67f-4307-811e-d1ccfb7c6d02_1775212393933.png	2026-04-09 22:00:00	2026-04-09 21:00:00	DIKONFIRMASI	50000	2026-04-03 17:34:49.004324	da633a33-0368-47c7-9ffb-07e6b0af2bda	7c9b649d-9377-46fb-b65b-b035d5e45587
4ac1ce4f-a336-4b32-8018-e4f5e45c8597	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 08:00:00	2026-04-09 07:00:00	DIKONFIRMASI	75000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
2f095bc8-df6f-4d5b-b241-ecafd08555c0	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 12:00:00	2026-04-09 11:00:00	DIKONFIRMASI	50000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
06293d5b-c0cc-4bf6-8ec1-8049365da950	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 14:00:00	2026-04-09 13:00:00	DIKONFIRMASI	50000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
8a77aed5-e197-44e4-ac69-6de2354f0eba	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 18:00:00	2026-04-09 17:00:00	DIKONFIRMASI	50000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
1d19c34e-931f-40d1-a62e-bb6dfbd4bd91	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 20:00:00	2026-04-09 19:00:00	DIKONFIRMASI	50000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
f40256c7-2a4f-4949-8755-3cf4ab5d9f71	8d607c90-b128-485a-86cd-897307307c5b	2026-04-03 17:38:36.854089	1	sher3	0866234323232	2026-04-03 17:48:36.854089	837b85ef-4c63-4a77-aab2-2b66f916cacb	proof_a83238e6-30e8-4943-92b5-72a1498ab341_1775212727125.png	2026-04-09 22:00:00	2026-04-09 21:00:00	DIKONFIRMASI	50000	2026-04-03 17:50:46.799589	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
2108265e-4996-4863-be1f-74f7cd92418b	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 09:00:00	2026-04-10 08:00:00	DIKONFIRMASI	75000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
4334d7a6-83e8-47d2-b522-2fcbec3f2d82	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 11:00:00	2026-04-10 10:00:00	DIKONFIRMASI	75000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
7e205739-33e5-4be3-b97b-79ce16372524	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 14:00:00	2026-04-10 13:00:00	DIKONFIRMASI	75000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
d4c225f3-d7ad-409c-8bf3-77d0ba32d8e6	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 16:00:00	2026-04-10 15:00:00	DIKONFIRMASI	50000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
432aa6b2-b8e9-47e0-abbc-de441113a93d	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 20:00:00	2026-04-10 19:00:00	DIKONFIRMASI	50000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
de045253-4f3d-4d5d-8324-505259eeb1f0	fa095931-677b-49e8-bfc8-516253a300f0	2026-04-03 17:52:27.349951	1	sher3	081234567	2026-04-03 18:02:27.349951	337129dc-e654-4267-ab07-01e7daf7e8b4	proof_a947e1d0-5936-4b0a-900a-591fd3dcc1fa_1775213555502.png	2026-04-10 22:00:00	2026-04-10 21:00:00	DIKONFIRMASI	50000	2026-04-03 17:53:40.39504	da633a33-0368-47c7-9ffb-07e6b0af2bda	aab66393-2f53-4172-9b27-b7677bf8f3ae
\.


--
-- Data for Name: reservasi_rent_list; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.reservasi_rent_list (reservasi_id, barang_id) FROM stdin;
28802534-10c7-4f62-ac9e-dc538cc7cb02	5c4a2a80-7d6e-48fb-a3bc-1a9925ece90b
621ab94e-a78f-4b7b-9066-0f2bc233bb73	0382eb10-3703-49b7-842a-889efe3f7640
621ab94e-a78f-4b7b-9066-0f2bc233bb73	0382eb10-3703-49b7-842a-889efe3f7640
e4e9d893-9cb6-402b-a2d2-235bcd861617	0382eb10-3703-49b7-842a-889efe3f7640
4ac1ce4f-a336-4b32-8018-e4f5e45c8597	0382eb10-3703-49b7-842a-889efe3f7640
2108265e-4996-4863-be1f-74f7cd92418b	0382eb10-3703-49b7-842a-889efe3f7640
4334d7a6-83e8-47d2-b522-2fcbec3f2d82	0382eb10-3703-49b7-842a-889efe3f7640
7e205739-33e5-4be3-b97b-79ce16372524	0382eb10-3703-49b7-842a-889efe3f7640
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.roles (id, role_name) FROM stdin;
1	GUEST
2	MEMBER
3	STAF_LAPANGAN
4	STAF_TOKO
5	OWNER
6	ADMIN
\.


--
-- Data for Name: transaksi; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.transaksi (id, created_at, diskon, grand_total, payment_method, staff_id, status, subtotal, updated_at) FROM stdin;
\.


--
-- Data for Name: transaksi_detail; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.transaksi_detail (id, harga_satuan, kuantitas, subtotal, barang_id, transaksi_id) FROM stdin;
\.


--
-- Data for Name: user_status; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.user_status (id, name) FROM stdin;
1	PENDING
2	AKTIF
3	NON_AKTIF
4	SUSPENDED
5	BANNED
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: gor-gemilangcondet-dev
--

COPY public.users (id, email, membership_end, membership_start, password, username, role_id, status_id, banned_at) FROM stdin;
52192372-04aa-424e-9062-cf929e91020f	guest1@example.com	\N	\N	$2a$10$.l0BUcncEb7AhMhJVKVoM.UFjO3Q5YUk6gR5fod6lhRar4tWcVayO	guest1	1	2	\N
da633a33-0368-47c7-9ffb-07e6b0af2bda	guest2@example.com	\N	\N	$2a$10$2c8kCc4BFS9CIg0LY4hdD.jFfUxAAj5dSMyJZ71CDIQ4hU/NS37qO	guest2	1	2	\N
00a7f484-4c2e-4b1f-837d-4288a3dcc861	guest3@example.com	\N	\N	$2a$10$2fKfr/LSQxjBzUhMMCUeQOVLRFFzfjjt4KFa7qSPhPEGM00/qU7bq	guest3	1	2	\N
8bf2172f-76b5-4d6c-9d69-6bfe45caefda	member1@example.com	\N	\N	$2a$10$zXaeK4CByr1poyZkjgbeE.9wqZDZ8fwyngb50I8vYNK8Ovi/bdSbG	member1	2	2	\N
bc7e5960-cfa0-46ce-a69b-02930bdfeb1a	member2@example.com	\N	\N	$2a$10$bQCOE7vx0UbJBLhLb8qA9efy9eMZs/N89PTOhgFpBRcskqv2aj0Wa	member2	2	2	\N
cd202bab-dd44-4d2b-9029-90745748ef41	member3@example.com	\N	\N	$2a$10$0oK8Miur/Q4/8l4gLtroAObWLxFRNsGcf.Z63INqm8OjWcuKppwdO	member3	2	2	\N
d9b9fccb-a175-4d91-8b8f-2b296588bf44	staf.lapangan1@example.com	\N	\N	$2a$10$5f0zZoUl62.AWZij0a6h7ugt3lTMajzDYBdZnPlYbFWdhYq/q98aq	staf_lapangan1	3	2	\N
6e93b343-7b7e-45c2-bafb-48680152b0e0	staf.lapangan2@example.com	\N	\N	$2a$10$CL9mENCpxpnpaQMZPdrFz.7nc3DUm1aTXLsP9WZ00.Qhb3TjmFLpK	staf_lapangan2	3	2	\N
856b8e24-179e-4d48-a33c-7cf972d8e122	staf.lapangan3@example.com	\N	\N	$2a$10$JHXI/5Nism6TALaZzUeBIOoEJvEDn1h8l647ZfBIseFaAF6FhG5n.	staf_lapangan3	3	2	\N
4988be45-f2d4-4a32-8888-b4b56a787bdf	staf.toko1@example.com	\N	\N	$2a$10$FYm.4IGKg9E6YMwIU1jW.eFQRMsyh2sDd6mIgjxcooH1aGXqOXZVG	staf_toko1	4	2	\N
f71e29e3-cef1-434a-ac7b-7297de78345b	staf.toko2@example.com	\N	\N	$2a$10$foikEfPlWqeRMMcLkZGvbekmyzeO7HC6dddTSmyf2dolylvlCzYWO	staf_toko2	4	2	\N
49133a21-056d-4921-a333-a5674f45af43	staf.toko3@example.com	\N	\N	$2a$10$TVaQ5PelmO728XQRg3QXf.yAheXqc1ndAjqPdS8tloP6KCKmAZPMa	staf_toko3	4	2	\N
93309ac1-856e-4a04-bf58-b7e2332b4588	owner1@example.com	\N	\N	$2a$10$CMC/Rrp9C40BBQrvyzXFPOhAlcFo6qYeGJvGZ8GCgE0GnXdxM2RfG	owner1	5	2	\N
221f2934-e6a2-4dc9-bfd7-9fa672d929fe	owner2@example.com	\N	\N	$2a$10$CP9ZQL/EjE9W1BHSKnEcDePtZoo4Wgqy3UDt5.aFBd4ADceY/aLAm	owner2	5	2	\N
ba127930-64b0-4bc0-a430-728e954618d3	owner3@example.com	\N	\N	$2a$10$H5H1qcrfiuZEBz0o3BZEzO.80GTpHfUrqFycMOtqoVF687Ay9nnIW	owner3	5	2	\N
51df8e59-6049-4348-8f30-8be6d49c0c94	admin1@example.com	\N	\N	$2a$10$Uzyf/sjaV6MiImrEyeu7Nefh9lFZdpl8qrsFAWHSg64r.qu3K8tJu	admin1	6	2	\N
f9223e6f-930a-425e-9c9e-a54aa9881d29	admin2@example.com	\N	\N	$2a$10$2SWbY2ErRm8JEfX4DGVnUeUUo4WAugXP.XKxXXe0X0DTvy5g9DHWC	admin2	6	2	\N
d9b6a313-06f2-4cf7-ac84-0b3eba618626	admin3@example.com	\N	\N	$2a$10$ij6KMEyE6Bhq6eZlWxOG9.MyQ77o5do6ogrkZF4cqflnZAwCrjGBu	admin3	6	2	\N
\.


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gor-gemilangcondet-dev
--

SELECT pg_catalog.setval('public.roles_id_seq', 6, true);


--
-- Name: user_status_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gor-gemilangcondet-dev
--

SELECT pg_catalog.setval('public.user_status_id_seq', 5, true);


--
-- Name: alat_olahraga alat_olahraga_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.alat_olahraga
    ADD CONSTRAINT alat_olahraga_pkey PRIMARY KEY (id);


--
-- Name: barang barang_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.barang
    ADD CONSTRAINT barang_pkey PRIMARY KEY (id);


--
-- Name: bookings bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT bookings_pkey PRIMARY KEY (id);


--
-- Name: courts courts_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.courts
    ADD CONSTRAINT courts_pkey PRIMARY KEY (id);


--
-- Name: lapangan_log lapangan_log_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.lapangan_log
    ADD CONSTRAINT lapangan_log_pkey PRIMARY KEY (id);


--
-- Name: lapangan lapangan_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.lapangan
    ADD CONSTRAINT lapangan_pkey PRIMARY KEY (id);


--
-- Name: pembayaran pembayaran_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.pembayaran
    ADD CONSTRAINT pembayaran_pkey PRIMARY KEY (id);


--
-- Name: reservasi reservasi_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.reservasi
    ADD CONSTRAINT reservasi_pkey PRIMARY KEY (id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: transaksi_detail transaksi_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.transaksi_detail
    ADD CONSTRAINT transaksi_detail_pkey PRIMARY KEY (id);


--
-- Name: transaksi transaksi_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.transaksi
    ADD CONSTRAINT transaksi_pkey PRIMARY KEY (id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: roles uk716hgxp60ym1lifrdgp67xt5k; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uk716hgxp60ym1lifrdgp67xt5k UNIQUE (role_name);


--
-- Name: barang ukabrg6ny9psv1dqidms49ns6cn; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.barang
    ADD CONSTRAINT ukabrg6ny9psv1dqidms49ns6cn UNIQUE (name);


--
-- Name: user_status ukbc74mmoktgh006h69dxwknjw9; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.user_status
    ADD CONSTRAINT ukbc74mmoktgh006h69dxwknjw9 UNIQUE (name);


--
-- Name: lapangan ukg4qcevmqe9530pn9qqu8i87tr; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.lapangan
    ADD CONSTRAINT ukg4qcevmqe9530pn9qqu8i87tr UNIQUE (kode);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: bookings uq_booking_lapangan_date_time; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT uq_booking_lapangan_date_time UNIQUE (lapangan_id, booking_date, start_time);


--
-- Name: user_status user_status_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.user_status
    ADD CONSTRAINT user_status_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users fk_users_status; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_status FOREIGN KEY (status_id) REFERENCES public.user_status(id);


--
-- Name: transaksi_detail fkayc594k4jf6anxufmbjdlfdh9; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.transaksi_detail
    ADD CONSTRAINT fkayc594k4jf6anxufmbjdlfdh9 FOREIGN KEY (barang_id) REFERENCES public.barang(id);


--
-- Name: alat_olahraga fkdo1yyfbsxgr9edipcxuf9kgxh; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.alat_olahraga
    ADD CONSTRAINT fkdo1yyfbsxgr9edipcxuf9kgxh FOREIGN KEY (id) REFERENCES public.barang(id);


--
-- Name: transaksi_detail fki2vrdxdk5y1b0ehtde13qgcjl; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.transaksi_detail
    ADD CONSTRAINT fki2vrdxdk5y1b0ehtde13qgcjl FOREIGN KEY (transaksi_id) REFERENCES public.transaksi(id);


--
-- Name: bookings fkkjov8h07c0ojdhv5liu0oggfv; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.bookings
    ADD CONSTRAINT fkkjov8h07c0ojdhv5liu0oggfv FOREIGN KEY (lapangan_id) REFERENCES public.lapangan(id);


--
-- Name: users fkp56c1712k691lhsyewcssf40f; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkp56c1712k691lhsyewcssf40f FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: reservasi fkpu3yi8dkqa2f5rxrjfgcwcdrm; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.reservasi
    ADD CONSTRAINT fkpu3yi8dkqa2f5rxrjfgcwcdrm FOREIGN KEY (lapangan_id) REFERENCES public.lapangan(id);


--
-- Name: lapangan_fasilitas fkqmwcuqcr83io4xfm1xr0twc3n; Type: FK CONSTRAINT; Schema: public; Owner: gor-gemilangcondet-dev
--

ALTER TABLE ONLY public.lapangan_fasilitas
    ADD CONSTRAINT fkqmwcuqcr83io4xfm1xr0twc3n FOREIGN KEY (lapangan_id) REFERENCES public.lapangan(id);


--
-- PostgreSQL database dump complete
--

\unrestrict dLPpdMXQxpcBMbPiusQ2GyBEkRFL9iXDC5awXBKPCovR88UGlbZAxsGSstiQiH8

