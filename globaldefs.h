
//each qr generated size allocated is this 4 bytes larger, to avoid mem corruption when the mem is not padded to 4
const int end_corruption_overhead = 4; // since bits write function (utils::set_data) can sometimes write on 4byte boundary and write to the end up to 3 extra bytes



