package Linked_List;

public class LinkedList {
    private Node head; // node dau tien
    private int size;
    //them vao dau danh sach
    public void addFirst(int value){
        Node newNode = new Node(value);
        newNode.next = head;
        head = newNode;   // cap nhat Node moi thanh Node dau tien
    }

    //them vao cuoi danh sach
    public void addLast(int value){
        Node newNode = new Node(value);
        if (head== null){   // neu danh sach rong
            head = newNode;  // thi Node moi se thanh Node dau
            return;
        }
        Node cur = head;    // cur la mot con tro tam thoi
        while (cur.next != null){   // dung vong lap nay de di den Node cuoi
            cur = cur.next;
        }
        cur.next = newNode;  // den Node cuoi them vao Node moi
    }

    //Xoa Node dau tien
    public void removeFirst(){
        if (head == null){
            return;
        }
        head = head.next;
    }

    //Xoa Node cuoi
    public void removeLast(){
        if (head == null){
            return;
        }
        if (head.next == null){
            head = null;   // neu chi co 1 cai thi xoa lun cai dau tien
            return;
        }
        Node cur = head;
        while (cur.next != null){
            cur = cur.next;
        }
        cur.next = null;
    }

    // Tim kiem tuyen tinh
    public int search ( int target){
        Node cur = head;
        int idx = 0;
        while (cur != null){
            if (cur.data == target){
                return idx;
            }
            cur = cur.next;
            idx++;
        }
        return -1;
    }


    //In danh sach
    public void printList(){
        Node cur = head;
        while (cur != null){
            System.out.print(cur.data + "->");
            cur = cur.next;
        }
        System.out.println("null");
    }
}
